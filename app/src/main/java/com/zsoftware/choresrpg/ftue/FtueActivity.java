package com.zsoftware.choresrpg.ftue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.DayActivity;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.ActivityFtueBinding;
import com.zsoftware.choresrpg.settings.AppSettings;
import com.zsoftware.choresrpg.settings.NotificationSettingsController;

// First-time user experience (FTUE): shown once, when the app first starts. Goes step by step:
// 1. welcome, 2. sign in or keep chores only on this phone, 3. notification settings, 4. all done.
// Back / Next move between steps; finishing marks it done so it isn't shown again. Leaving early
// (back on the first step) shows it again next time.
public class FtueActivity extends AppCompatActivity {

    private static final int StepWelcome = 0;
    private static final int StepAccount = 1;
    private static final int StepSettings = 2;
    private static final int StepDone = 3;
    private static final int StepCount = 4;
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

        binding.backButton.setOnClickListener(view -> ShowStep(Step - 1));
        binding.nextButton.setOnClickListener(view -> OnNext());
        binding.signInButton.setOnClickListener(view -> SignIn());
        binding.localOnlyButton.setOnClickListener(view -> ShowStep(StepSettings));

        // System back goes to the previous step; on the first step it leaves the setup.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Step > StepWelcome)
                    ShowStep(Step - 1);
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
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(StateStep, Step);
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
        ShowStep(Step + 1);
    }

    private void ShowStep(int step)
    {
        Step = Math.max(StepWelcome, Math.min(StepDone, step));
        binding.stepWelcome.setVisibility(Step == StepWelcome ? View.VISIBLE : View.GONE);
        binding.stepAccount.setVisibility(Step == StepAccount ? View.VISIBLE : View.GONE);
        binding.stepSettings.setVisibility(Step == StepSettings ? View.VISIBLE : View.GONE);
        binding.stepDone.setVisibility(Step == StepDone ? View.VISIBLE : View.GONE);

        binding.stepIndicator.setText(getString(R.string.ftue_step, Step + 1, StepCount));
        binding.backButton.setVisibility(Step == StepWelcome ? View.INVISIBLE : View.VISIBLE);
        binding.nextButton.setText(Step == StepDone ? R.string.ftue_get_started : R.string.ftue_next);

        if (Step == StepAccount)
            ShowAccount();
        if (Step == StepSettings)
            Notifications.Show();
    }

    // Signed in: shows the account and hides the choice buttons (Next carries on). Otherwise the two choices.
    private void ShowAccount()
    {
        boolean signedIn = AccountManager.IsSignedIn();
        binding.accountStatus.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        if (signedIn)
            binding.accountStatus.setText(getString(R.string.settings_signed_in_as, AccountManager.GetUserLabel()));
        binding.signInButton.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.localOnlyButton.setVisibility(signedIn ? View.GONE : View.VISIBLE);
        binding.signInButton.setEnabled(true);
        binding.localOnlyButton.setEnabled(true);
    }

    private void SignIn()
    {
        binding.signInButton.setEnabled(false);
        binding.localOnlyButton.setEnabled(false);
        AccountManager.SignIn(this, result -> {
            ShowAccount();
            if (result == AccountManager.SignInResult.SignedIn)
            {
                // Uploads this phone's chores and downloads the account's chores.
                ChoreSync.Start(this);
                ShortToast.Show(this, R.string.signed_in);
                ShowStep(StepSettings);
            }
            else if (result == AccountManager.SignInResult.NoGoogleAccount)
                ShortToast.Show(this, R.string.sign_in_no_google_account);
            else if (result == AccountManager.SignInResult.Failed)
                ShortToast.Show(this, R.string.sign_in_failed);
        });
    }
}
