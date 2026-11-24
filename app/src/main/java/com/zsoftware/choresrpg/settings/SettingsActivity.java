package com.zsoftware.choresrpg.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.ActivitySettingsBinding;
import com.zsoftware.choresrpg.reminders.ChoreReminders;

import java.util.Calendar;

// Settings: the account (sign in with Google), daily chore notifications on/off and the morning /
// evening times they are sent at. Opened from the ⋮ menu of the week and month views.
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
        ChoreSync.Start(this);
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
        binding.accountButton.setOnClickListener(view -> OnAccountButton());
        binding.deleteAccountButton.setOnClickListener(view -> OnDeleteAccountButton());
    }

    // Signed out: "Not signed in" + Sign in with Google. Signed in: the account + Sign out, and Delete account.
    private void ShowAccount()
    {
        boolean signedIn = AccountManager.IsSignedIn();
        binding.accountTitle.setText(signedIn
                ? getString(R.string.settings_signed_in_as, AccountManager.GetUserLabel())
                : getString(R.string.settings_not_signed_in));
        binding.accountSummary.setText(signedIn ? R.string.settings_signed_in_summary : R.string.settings_sign_in_summary);
        binding.accountButton.setText(signedIn ? R.string.settings_sign_out : R.string.settings_sign_in);
        binding.deleteAccountButton.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        SetAccountButtonsEnabled(true);
    }

    // Off while Google's picker or Firebase is working, so nothing is started twice.
    private void SetAccountButtonsEnabled(boolean enabled)
    {
        binding.accountButton.setEnabled(enabled);
        binding.deleteAccountButton.setEnabled(enabled);
    }

    private void OnAccountButton()
    {
        if (AccountManager.IsSignedIn())
        {
            AskKeepLocalChores(R.string.sign_out_title, R.string.sign_out_keep_message, this::SignOut);
            return;
        }
        SetAccountButtonsEnabled(false);
        AccountManager.SignIn(this, result -> {
            if (result == AccountManager.SignInResult.SignedIn)
            {
                // Uploads this phone's chores and downloads the account's chores.
                ChoreSync.Start(this);
                ShortToast.Show(this, R.string.signed_in);
            }
            else if (result == AccountManager.SignInResult.NoGoogleAccount)
                ShortToast.Show(this, R.string.sign_in_no_google_account);
            else if (result == AccountManager.SignInResult.Failed)
                ShortToast.Show(this, R.string.sign_in_failed);
            ShowAccount();
        });
    }

    public interface OnKeepLocalChoresAnswered
    {
        void Answered(boolean keepLocalChores);
    }

    // Popup: keep a copy of the chores on this phone, or remove them from it? Cancel does nothing.
    private void AskKeepLocalChores(int titleId, int messageId, OnKeepLocalChoresAnswered onAnswered)
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.keep_on_phone, (dialog, which) -> onAnswered.Answered(true))
                .setNegativeButton(R.string.remove_from_phone, (dialog, which) -> onAnswered.Answered(false))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void SignOut(boolean keepLocalChores)
    {
        SetAccountButtonsEnabled(false);
        // Stop syncing first, so removing the phone's chores isn't sent to the cloud as deletions.
        ChoreSync.Stop();
        if (!keepLocalChores)
            SaveData.Instance().ClearLocalChores();
        AccountManager.SignOut(this, () -> {
            ShortToast.Show(this, R.string.signed_out);
            ShowAccount();
        });
    }

    // Popup 1: confirm deleting the account (red Delete button). Popup 2: keep the chores on this phone?
    private void OnDeleteAccountButton()
    {
        // The bin icon is drawn white; tint it red to match the Delete button.
        Drawable binIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete).mutate();
        binIcon.setTint(ContextCompat.getColor(this, R.color.delete_red));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setIcon(binIcon)
                .setTitle(R.string.delete_account_title)
                .setMessage(getString(R.string.delete_account_message, AccountManager.GetUserLabel()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, which) ->
                        AskKeepLocalChores(R.string.delete_account_keep_title, R.string.delete_account_keep_message, this::DeleteAccount))
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.delete_red));
    }

    private void DeleteAccount(boolean keepLocalChores)
    {
        SetAccountButtonsEnabled(false);
        // Stop syncing first: the cloud chores are about to be deleted and must not come back from (or be
        // re-uploaded by) this phone.
        ChoreSync.Stop();
        AccountManager.DeleteAccount(this, result -> {
            if (result == AccountManager.DeleteResult.Deleted)
            {
                if (!keepLocalChores)
                    SaveData.Instance().ClearLocalChores();
                ShortToast.Show(this, R.string.account_deleted);
            }
            else
            {
                // Still signed in: carry on syncing.
                ChoreSync.Start(this);
                if (result == AccountManager.DeleteResult.Failed)
                    ShortToast.Show(this, R.string.delete_account_failed);
            }
            ShowAccount();
        });
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
        ShowAccount();

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
