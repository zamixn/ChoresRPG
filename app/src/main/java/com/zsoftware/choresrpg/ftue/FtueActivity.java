package com.zsoftware.choresrpg.ftue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.DayActivity;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.ActivityFtueBinding;
import com.zsoftware.choresrpg.group.GroupDialogs;
import com.zsoftware.choresrpg.settings.AppSettings;
import com.zsoftware.choresrpg.settings.NotificationSettingsController;

import java.util.ArrayList;
import java.util.List;

// First-time user experience (FTUE): shown once, when the app first starts. Goes step by step:
// 1. welcome, 2. sign in (or out) or keep chores only on this phone, 3. groups - only while signed in:
// what groups are, and creating one, 4. notification settings, 5. all done.
// Back / Next move between steps; finishing marks it done so it isn't shown again. Leaving early
// (back on the first step) shows it again next time.
public class FtueActivity extends AppCompatActivity {

    private static final int StepWelcome = 0;
    private static final int StepAccount = 1;
    private static final int StepGroups = 2;
    private static final int StepSettings = 3;
    private static final int StepDone = 4;
    private static final String StateStep = "Step";

    private ActivityFtueBinding binding;
    private NotificationSettingsController Notifications;
    private int Step = StepWelcome;

    public static Intent CreateIntent(Context context)
    {
        return new Intent(context, FtueActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Light screen without a toolbar: let Android pick dark or light system bar icons.
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);
        AppSettings.Initialize(this);

        binding = ActivityFtueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsetsToScreen(binding.ftueRoot);

        Notifications = new NotificationSettingsController(this, binding.notificationSettings);

        binding.backButton.setOnClickListener(view -> ShowStep(GetPreviousStep()));
        binding.nextButton.setOnClickListener(view -> OnNext());
        binding.signInButton.setOnClickListener(view -> SignIn());
        binding.signOutButton.setOnClickListener(view -> AskSignOut());
        binding.localOnlyButton.setOnClickListener(view -> ShowStep(StepSettings));
        binding.createGroupButton.setOnClickListener(view ->
                GroupDialogs.CreateGroup(this, true, groupId -> ShowGroups()));

        // System back goes to the previous step; on the first step it leaves the setup.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Step > StepWelcome)
                    ShowStep(GetPreviousStep());
                else
                    finish();
            }
        });

        if (savedInstanceState != null)
            Step = savedInstanceState.getInt(StateStep, StepWelcome);
        ShowStep(Step);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Notifications.Show();
        GroupManager.AddListener(GroupChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GroupManager.RemoveListener(GroupChanged);
    }

    // The groups loaded or changed: update the group step.
    private final GroupManager.OnGroupChanged GroupChanged = new GroupManager.OnGroupChanged() {
        @Override
        public void GroupChanged() {
            if (Step == StepGroups)
                ShowGroups();
        }
    };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(StateStep, Step);
    }

    // The steps in order; the group step only while signed in.
    private List<Integer> GetSteps()
    {
        List<Integer> steps = new ArrayList<>();
        steps.add(StepWelcome);
        steps.add(StepAccount);
        if (AccountManager.IsSignedIn())
            steps.add(StepGroups);
        steps.add(StepSettings);
        steps.add(StepDone);
        return steps;
    }

    private int GetPreviousStep()
    {
        List<Integer> steps = GetSteps();
        int index = steps.indexOf(Step);
        return index <= 0 ? StepWelcome : steps.get(index - 1);
    }

    private int GetNextStep()
    {
        List<Integer> steps = GetSteps();
        int index = steps.indexOf(Step);
        return index < 0 || index >= steps.size() - 1 ? StepDone : steps.get(index + 1);
    }

    private void OnNext()
    {
        if (Step == StepDone)
        {
            AppSettings.Instance().SetFtueCompleted(true);
            // Done: continue in the daily view (the app's default view), on top of the week view, with a
            // short tutorial of its buttons.
            startActivity(DayActivity.CreateDailyViewWithTutorialIntent(this));
            finish();
            return;
        }
        ShowStep(GetNextStep());
    }

    private void ShowStep(int step)
    {
        List<Integer> steps = GetSteps();
        // e.g. the group step after signing out: back to the account step.
        if (!steps.contains(step))
            step = step == StepGroups ? StepAccount : StepWelcome;
        Step = step;
        binding.stepWelcome.setVisibility(Step == StepWelcome ? View.VISIBLE : View.GONE);
        binding.stepAccount.setVisibility(Step == StepAccount ? View.VISIBLE : View.GONE);
        binding.stepGroups.setVisibility(Step == StepGroups ? View.VISIBLE : View.GONE);
        binding.stepSettings.setVisibility(Step == StepSettings ? View.VISIBLE : View.GONE);
        binding.stepDone.setVisibility(Step == StepDone ? View.VISIBLE : View.GONE);

        binding.stepIndicator.setText(getString(R.string.ftue_step, steps.indexOf(Step) + 1, steps.size()));
        binding.backButton.setVisibility(Step == StepWelcome ? View.INVISIBLE : View.VISIBLE);
        binding.nextButton.setText(Step == StepDone ? R.string.ftue_get_started : R.string.ftue_next);

        if (Step == StepAccount)
            ShowAccount();
        if (Step == StepGroups)
            ShowGroups();
        if (Step == StepSettings)
            Notifications.Show();
    }

    // Signed in: shows the account and a sign-out button (Next carries on). Otherwise the two choices.
    private void ShowAccount()
    {
        boolean signedIn = AccountManager.IsSignedIn();
        binding.accountStatus.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn)
            binding.accountStatus.setText(getString(R.string.settings_signed_in_as, AccountManager.GetUserLabel()));
        binding.signInButton.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.localOnlyButton.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.signOutButton.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        SetAccountButtonsEnabled(true);
        // The step count changes with the group step.
        binding.stepIndicator.setText(getString(R.string.ftue_step, GetSteps().indexOf(Step) + 1, GetSteps().size()));
    }

    private void SetAccountButtonsEnabled(boolean enabled)
    {
        binding.signInButton.setEnabled(enabled);
        binding.localOnlyButton.setEnabled(enabled);
        binding.signOutButton.setEnabled(enabled);
    }

    private void SignIn()
    {
        SetAccountButtonsEnabled(false);
        AccountManager.SignIn(this, result -> {
            ShowAccount();
            if (result == AccountManager.SignInResult.SignedIn)
            {
                // Finds (or creates) the user's groups, uploads this phone's chores and downloads theirs.
                ChoreSync.Start(this);
                ShortToast.Show(this, R.string.signed_in);
                ShowStep(StepGroups);
            }
            else if (result == AccountManager.SignInResult.NoGoogleAccount)
                ShortToast.Show(this, R.string.sign_in_no_google_account);
            else if (result == AccountManager.SignInResult.Failed)
                ShortToast.Show(this, R.string.sign_in_failed);
        });
    }

    // Same as in Settings: keep a copy of the chores on this phone, or remove them from it?
    private void AskSignOut()
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sign_out_title)
                .setMessage(R.string.sign_out_keep_message)
                .setPositiveButton(R.string.keep_on_phone, (dialog, which) -> SignOut(true))
                .setNegativeButton(R.string.remove_from_phone, (dialog, which) -> SignOut(false))
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
        // Signed out of Firebase as soon as this returns; only forgetting the remembered Google account
        // finishes later (and may take a while), so the step shows the signed-out state right away.
        AccountManager.SignOut(this, () -> { });
        ShortToast.Show(this, R.string.signed_out);
        ShowAccount();
    }

    // The user's groups (main one marked) and the create button, once the groups are known.
    private void ShowGroups()
    {
        boolean loaded = GroupManager.IsLoaded();
        binding.createGroupButton.setEnabled(loaded);
        if (!loaded)
        {
            binding.groupsStatus.setText(GroupManager.HasLoadFailed() ? R.string.group_load_failed : R.string.group_loading);
            return;
        }
        String mainGroupId = GroupManager.GetMainGroupId();
        List<String> names = new ArrayList<>();
        for (Group group : GroupManager.GetGroups())
            names.add(group.Id.equals(mainGroupId) ? getString(R.string.ftue_groups_main, group.Name) : group.Name);
        binding.groupsStatus.setText(getString(R.string.ftue_groups_list, TextUtils.join(", ", names)));
    }
}
