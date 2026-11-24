package com.zsoftware.choresrpg.reminders;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zsoftware.choresrpg.settings.AppSettings;

// Woken by the morning / evening alarm: shows today's chore notification and sets the next alarm.
public class ReminderAlarmReceiver extends BroadcastReceiver {

    private static final String ExtraKind = "Kind";

    // The alarm for one notification (ChoreReminders.Morning or Evening); one per kind, so they don't replace each other.
    public static PendingIntent CreatePendingIntent(Context context, int kind)
    {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        intent.putExtra(ExtraKind, kind);
        return PendingIntent.getBroadcast(context, kind, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppSettings.Initialize(context);
        if (AppSettings.Instance().AreNotificationsEnabled())
            ChoreReminders.ShowTodayNotification(context, intent.getIntExtra(ExtraKind, ChoreReminders.Morning));
        // Alarms fire once; set tomorrow's.
        ChoreReminders.Schedule(context);
    }
}
