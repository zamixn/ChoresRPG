package com.zsoftware.choresrpg.settings;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
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
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.group.GroupDialogs;
import com.zsoftware.choresrpg.group.GroupListActivity;
import com.zsoftware.choresrpg.databinding.ActivitySettingsBinding;

import java.util.ArrayList;
import java.util.List;

// Settings: the account (sign in with Google), daily chore notifications on/off and the morning /
// evening times they are sent at. Opened from the ⋮ menu of the week and month views.
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private NotificationSettingsController Notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentSettings);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Notifications = new NotificationSettingsController(this, binding.notificationSettings);
        binding.testNotificationButton.setOnClickListener(view -> Notifications.SendTestNotification());
        binding.accountButton.setOnClickListener(view -> OnAccountButton());
        binding.deleteAccountButton.setOnClickListener(view -> OnDeleteAccountButton());
        binding.groupRow.setOnClickListener(view -> OpenGroups());
        binding.groupButton.setOnClickListener(view -> OpenGroups());
        binding.mainGroupRow.setOnClickListener(view -> ChooseMainGroup());
        binding.mainGroupButton.setOnClickListener(view -> ChooseMainGroup());
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
        ShowGroupRow();
    }

    // Groups need a Google account: signed out, a toast says so instead of opening the group list.
    private void OpenGroups()
    {
        if (!AccountManager.IsSignedIn())
        {
            ShortToast.Show(this, R.string.group_sign_in_needed);
            return;
        }
        startActivity(GroupListActivity.CreateIntent(this));
    }

    // Picks the group new chores go into by default.
    private void ChooseMainGroup()
    {
        if (!GroupManager.IsLoaded())
            return;
        GroupDialogs.ChooseGroup(this, R.string.settings_main_group, GroupManager.GetMainGroupId(), GroupManager::SetMainGroup);
    }

    // The groups' names (or why there are none), and the main group (signed in only).
    private void ShowGroupRow()
    {
        boolean signedIn = AccountManager.IsSignedIn();
        binding.mainGroupRow.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (!signedIn)
        {
            binding.groupSummary.setText(R.string.settings_group_signed_out);
            return;
        }
        if (!GroupManager.IsLoaded())
        {
            int text = GroupManager.HasLoadFailed() ? R.string.group_load_failed : R.string.group_loading;
            binding.groupSummary.setText(text);
            binding.mainGroupSummary.setText(text);
            binding.mainGroupButton.setEnabled(false);
            return;
        }
        List<String> names = new ArrayList<>();
        for (Group group : GroupManager.GetGroups())
            names.add(group.Name);
        binding.groupSummary.setText(TextUtils.join(", ", names));
        Group mainGroup = GroupManager.GetMainGroup();
        binding.mainGroupSummary.setText(mainGroup == null ? "" : getString(R.string.settings_main_group_summary, mainGroup.Name));
        binding.mainGroupButton.setEnabled(GroupManager.GetGroups().size() > 1);
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
        GroupManager.Clear();
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
        GroupManager.AddListener(GroupChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GroupManager.RemoveListener(GroupChanged);
    }

    // The groups were loaded / renamed / joined / left: show them.
    private final GroupManager.OnGroupChanged GroupChanged = this::ShowGroupRow;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowSettings()
    {
        ShowAccount();
        ShowGroupRow();
        Notifications.Show();
    }
}
