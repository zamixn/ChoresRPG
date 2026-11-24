package com.zsoftware.choresrpg.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.databinding.ActivitySettingsBinding;
import com.zsoftware.choresrpg.reminders.ChoreReminders;

import java.util.Calendar;

// Settings: daily chore notifications on/off and the morning / evening times they are sent at.
// Opened from the ⋮ menu of the week and month views.
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private AppSettings Settings;

    // Android 13+: asks for the notification permission when the user turns notifications on.
    private final ActivityResultLauncher<String> NotificationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted)
                    SetNotificationsEnabled(true);
                else
                {
                    ShortToast.Show(this, R.string.notifications_blocked);
                    ShowSettings();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        AppSettings.Initialize(this);
        Settings = AppSettings.Instance();

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentSettings);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.notificationsRow.setOnClickListener(view -> binding.notificationsSwitch.toggle());
        binding.morningTimeRow.setOnClickListener(view -> ShowTimePicker(true));
        binding.eveningTimeRow.setOnClickListener(view -> ShowTimePicker(false));
        binding.testNotificationButton.setOnClickListener(view -> SendTestNotification());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ShowSettings();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowSettings()
    {
        // Notifications count as on only while Android still allows them.
        boolean enabled = Settings.AreNotificationsEnabled() && ChoreReminders.CanPostNotifications(this);
        binding.notificationsSwitch.setOnCheckedChangeListener(null);
        binding.notificationsSwitch.setChecked(enabled);
        binding.notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                OnNotificationsSwitched(isChecked);
            }
        });

        binding.morningTimeValue.setText(FormatTime(Settings.GetMorningTime()));
        binding.eveningTimeValue.setText(FormatTime(Settings.GetEveningTime()));
        // The times only matter while notifications are on.
        float timesAlpha = enabled ? 1f : 0.4f;
        binding.morningTimeRow.setAlpha(timesAlpha);
        binding.eveningTimeRow.setAlpha(timesAlpha);
    }

    private void OnNotificationsSwitched(boolean isChecked)
    {
        if (isChecked && !ChoreReminders.CanPostNotifications(this)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            NotificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        if (isChecked && !ChoreReminders.CanPostNotifications(this))
        {
            // Turned off for this app in Android's settings (before Android 13 there is no request).
            ShortToast.Show(this, R.string.notifications_blocked);
            ShowSettings();
            return;
        }
        SetNotificationsEnabled(isChecked);
    }

    private void SetNotificationsEnabled(boolean enabled)
    {
        Settings.SetNotificationsEnabled(enabled);
        ChoreReminders.Schedule(this);
        ShowSettings();
    }

    private void ShowTimePicker(boolean morning)
    {
        int minutes = morning ? Settings.GetMorningTime() : Settings.GetEveningTime();
        new TimePickerDialog(this, (picker, hour, minute) -> {
            int picked = hour * 60 + minute;
            if (morning)
                Settings.SetMorningTime(picked);
            else
                Settings.SetEveningTime(picked);
            ChoreReminders.Schedule(this);
            ShowSettings();
        }, minutes / 60, minutes % 60, DateFormat.is24HourFormat(this)).show();
    }

    // Shows today's notification right away, to check how it looks.
    private void SendTestNotification()
    {
        if (!ChoreReminders.CanPostNotifications(this))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                NotificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            else
                ShortToast.Show(this, R.string.notifications_blocked);
            return;
        }
        if (!ChoreReminders.ShowTodayNotification(this, ChoreReminders.Morning))
            ShortToast.Show(this, R.string.reminder_no_chores_today);
    }

    // e.g. "10:00" or "10:00 AM", following the phone's 24-hour setting.
    private String FormatTime(int minutesAfterMidnight)
    {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60);
        time.set(Calendar.MINUTE, minutesAfterMidnight % 60);
        return DateFormat.getTimeFormat(this).format(time.getTime());
    }
}
