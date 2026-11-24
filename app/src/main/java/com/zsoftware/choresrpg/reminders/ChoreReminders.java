package com.zsoftware.choresrpg.reminders;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.zsoftware.choresrpg.AppClock;
import com.zsoftware.choresrpg.Chore;
import com.zsoftware.choresrpg.DayActivity;
import com.zsoftware.choresrpg.MainActivity;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.WeekDay;
import com.zsoftware.choresrpg.settings.AppSettings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Daily chore notifications: one notification listing today's chores (done ones with a checkmark,
// the rest with an empty box), sent at the morning and evening times from the settings.
public class ChoreReminders {
    private static String LOGTAG = "ChoreReminders";

    public static final int Morning = 0;
    public static final int Evening = 1;

    private static final String ChannelId = "DailyChores";
    // One id for both notifications, so the evening one replaces the morning one.
    private static final int NotificationId = 1;

    // Sets (or cancels, when notifications are off) the alarms for the next morning and evening
    // notification. Called on app start, when settings change, after each notification and after a reboot.
    public static void Schedule(Context context)
    {
        AppSettings.Initialize(context);
        AppSettings settings = AppSettings.Instance();
        ScheduleAlarm(context, Morning, settings.GetMorningTime(), settings.AreNotificationsEnabled());
        ScheduleAlarm(context, Evening, settings.GetEveningTime(), settings.AreNotificationsEnabled());
    }

    private static void ScheduleAlarm(Context context, int kind, int minutesAfterMidnight, boolean enabled)
    {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null)
            return;
        PendingIntent alarm = ReminderAlarmReceiver.CreatePendingIntent(context, kind);
        if (!enabled)
        {
            alarms.cancel(alarm);
            return;
        }
        // Not exact (that needs an extra permission): Android may deliver it a few minutes late to save
        // battery, but it also fires while the phone is idle.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, GetNextTime(minutesAfterMidnight, System.currentTimeMillis()), alarm);
    }

    // The next moment (after 'nowMillis') when the local clock shows the given time. Uses the real
    // clock, not the simulated day, because the alarm itself goes off in real time.
    public static long GetNextTime(int minutesAfterMidnight, long nowMillis)
    {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(nowMillis);
        next.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60);
        next.set(Calendar.MINUTE, minutesAfterMidnight % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= nowMillis)
            next.add(Calendar.DAY_OF_MONTH, 1);
        return next.getTimeInMillis();
    }

    // Lines of the notification for a day: unfinished chores first, then the completed ones.
    public static List<String> BuildChoreLines(List<Chore> chores, Calendar day, String todoFormat, String doneFormat)
    {
        WeekDay weekDay = WeekDay.FromCalendar(day);
        List<String> todo = new ArrayList<>();
        List<String> done = new ArrayList<>();
        for (Chore chore : chores)
        {
            if (!chore.IsOnDay(weekDay))
                continue;
            if (chore.IsCompletedOn(day))
                done.add(String.format(doneFormat, chore.GetDescription()));
            else
                todo.add(String.format(todoFormat, chore.GetDescription()));
        }
        todo.addAll(done);
        return todo;
    }

    // Shows the notification for today's chores (the simulated day in debug builds). Does nothing when
    // there are no chores today or notifications aren't allowed.
    public static void ShowTodayNotification(Context context, int kind)
    {
        SaveData.Initialize(context);
        Calendar today = AppClock.Now();
        List<Chore> chores = SaveData.Instance().GetChoreList();
        List<String> lines = BuildChoreLines(chores, today,
                context.getString(R.string.reminder_line_todo), context.getString(R.string.reminder_line_done));
        if (lines.isEmpty())
            return;

        int doneCount = 0;
        for (Chore chore : chores)
        {
            if (chore.IsOnDay(WeekDay.FromCalendar(today)) && chore.IsCompletedOn(today))
                doneCount++;
        }

        CreateChannel(context);
        String title = context.getString(kind == Morning ? R.string.reminder_title_morning : R.string.reminder_title_evening);
        String summary = context.getString(R.string.reminder_summary, doneCount, lines.size());
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, ChannelId)
                .setSmallIcon(R.drawable.ic_complete)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setSummaryText(summary)
                        .bigText(String.join("\n", lines)))
                .setContentIntent(CreateOpenTodayIntent(context, today))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return;
        try
        {
            NotificationManagerCompat.from(context).notify(NotificationId, notification.build());
        }
        catch (SecurityException e)
        {
            // Permission taken away between the check and the call.
            Log.e(LOGTAG, "ShowTodayNotification: not allowed to post", e);
        }
    }

    // Android 13+ needs the user's permission to show notifications.
    public static boolean CanPostNotifications(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false;
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static void CreateChannel(Context context)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        NotificationChannel channel = new NotificationChannel(ChannelId,
                context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null)
            manager.createNotificationChannel(channel);
    }

    // Tapping the notification opens today's day view, with the week view behind it.
    private static PendingIntent CreateOpenTodayIntent(Context context, Calendar today)
    {
        return TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, MainActivity.class))
                .addNextIntent(DayActivity.CreateIntent(context, WeekDay.FromCalendar(today), today))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
