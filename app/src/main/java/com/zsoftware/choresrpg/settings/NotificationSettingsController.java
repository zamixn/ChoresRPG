package com.zsoftware.choresrpg.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.os.Build;
import android.text.format.DateFormat;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.databinding.NotificationSettingsBinding;
import com.zsoftware.choresrpg.reminders.ChoreReminders;

import java.util.Calendar;

// Runs the notification settings rows (notification_settings.xml): the on/off switch (asking Android
// for permission when needed) and the morning / evening time pickers. Used by the Settings screen and
// the first-time setup.
public class NotificationSettingsController {

    private final AppCompatActivity Activity;
    private final NotificationSettingsBinding Views;
    private final AppSettings Settings;
    // Android 13+: asks for the notification permission when notifications are turned on.
    private final ActivityResultLauncher<String> PermissionRequest;
    // True while asking for permission for the test notification (not for turning notifications on).
    private boolean AskingForTestNotification = false;

    // Create in the activity's onCreate (Android only allows permission requests to be set up then).
    public NotificationSettingsController(AppCompatActivity activity, NotificationSettingsBinding views)
    {
        Activity = activity;
        Views = views;
        AppSettings.Initialize(activity);
        Settings = AppSettings.Instance();
        PermissionRequest = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            boolean forTest = AskingForTestNotification;
            AskingForTestNotification = false;
            if (!granted)
            {
                ShortToast.Show(Activity, R.string.notifications_blocked);
                Show();
            }
            else if (forTest)
                SendTestNotification();
            else
                SetNotificationsEnabled(true);
        });

        Views.notificationsRow.setOnClickListener(view -> Views.notificationsSwitch.toggle());
        Views.morningTimeRow.setOnClickListener(view -> ShowTimePicker(true));
        Views.eveningTimeRow.setOnClickListener(view -> ShowTimePicker(false));
        Views.editMorningTimeButton.setOnClickListener(view -> ShowTimePicker(true));
        Views.editEveningTimeButton.setOnClickListener(view -> ShowTimePicker(false));
    }

    // Shows the current settings (call from onResume: the permission may have changed meanwhile).
    public void Show()
    {
        // Notifications count as on only while Android still allows them.
        boolean enabled = Settings.AreNotificationsEnabled() && ChoreReminders.CanPostNotifications(Activity);
        Views.notificationsSwitch.setOnCheckedChangeListener(null);
        Views.notificationsSwitch.setChecked(enabled);
        Views.notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                OnNotificationsSwitched(isChecked);
            }
        });

        // The times can be changed whether or not notifications are on.
        Views.morningTimeValue.setText(FormatTime(Settings.GetMorningTime()));
        Views.eveningTimeValue.setText(FormatTime(Settings.GetEveningTime()));
    }

    private void OnNotificationsSwitched(boolean isChecked)
    {
        if (isChecked && !ChoreReminders.CanPostNotifications(Activity))
        {
            RequestPermission(false);
            return;
        }
        SetNotificationsEnabled(isChecked);
    }

    // Asks Android for permission (13+); on older versions it can only be allowed in Android's settings.
    private void RequestPermission(boolean forTestNotification)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            AskingForTestNotification = forTestNotification;
            PermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        ShortToast.Show(Activity, R.string.notifications_blocked);
        Show();
    }

    private void SetNotificationsEnabled(boolean enabled)
    {
        Settings.SetNotificationsEnabled(enabled);
        ChoreReminders.Schedule(Activity);
        Show();
    }

    private void ShowTimePicker(boolean morning)
    {
        int minutes = morning ? Settings.GetMorningTime() : Settings.GetEveningTime();
        new TimePickerDialog(Activity, (picker, hour, minute) -> {
            int picked = hour * 60 + minute;
            if (morning)
                Settings.SetMorningTime(picked);
            else
                Settings.SetEveningTime(picked);
            ChoreReminders.Schedule(Activity);
            Show();
        }, minutes / 60, minutes % 60, DateFormat.is24HourFormat(Activity)).show();
    }

    // Shows today's notification right away, to check how it looks.
    public void SendTestNotification()
    {
        if (!ChoreReminders.CanPostNotifications(Activity))
        {
            RequestPermission(true);
            return;
        }
        if (!ChoreReminders.ShowTodayNotification(Activity, ChoreReminders.Morning))
            ShortToast.Show(Activity, R.string.reminder_no_chores_today);
    }

    // e.g. "10:00" or "10:00 AM", following the phone's 24-hour setting.
    private String FormatTime(int minutesAfterMidnight)
    {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60);
        time.set(Calendar.MINUTE, minutesAfterMidnight % 60);
        return DateFormat.getTimeFormat(Activity).format(time.getTime());
    }
}
