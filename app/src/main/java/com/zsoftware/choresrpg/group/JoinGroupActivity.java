package com.zsoftware.choresrpg.group;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.DayActivity;
import com.zsoftware.choresrpg.MainActivity;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.ActivityJoinGroupBinding;

// Opened by a group invite link (choresrpg://join/CODE) or by "Join with a code" in the group list:
// asks the user to confirm joining, signing in first if needed. The user stays in their other groups.
public class JoinGroupActivity extends AppCompatActivity {

    private static final String ExtraCode = "InviteCode";

    private ActivityJoinGroupBinding binding;
    private String Code;
    private GroupManager.InviteInfo Invite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);

        binding = ActivityJoinGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsetsToScreen(binding.joinRoot);

        binding.cancelButton.setOnClickListener(view -> finish());
        Code = getIntent().hasExtra(ExtraCode) ? getIntent().getStringExtra(ExtraCode) : GetCode(getIntent().getData());
        if (Code == null)
        {
            ShowInvalid();
            return;
        }
        if (!AccountManager.IsSignedIn())
            ShowSignIn();
        else
            LookUpInvite();
    }

    // Opens the join screen for a code typed in the app.
    public static Intent CreateIntent(Context context, String code)
    {
        Intent intent = new Intent(context, JoinGroupActivity.class);
        intent.putExtra(ExtraCode, code);
        return intent;
    }

    // The invite code: the last part of the link's path (choresrpg://join/CODE).
    private static String GetCode(Uri link)
    {
        if (link == null || link.getLastPathSegment() == null)
            return null;
        return GroupManager.ParseInviteCode(link.getLastPathSegment());
    }

    private void ShowBusy(boolean busy)
    {
        binding.joinProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.joinButton.setEnabled(!busy);
    }

    // Groups need a Google account: sign in here first.
    private void ShowSignIn()
    {
        binding.joinTitle.setText(R.string.group_join_title_unknown);
        binding.joinText.setText(R.string.group_join_sign_in_text);
        binding.joinButton.setText(R.string.settings_sign_in);
        binding.joinButton.setOnClickListener(view -> {
            ShowBusy(true);
            AccountManager.SignIn(this, result -> {
                ShowBusy(false);
                if (result == AccountManager.SignInResult.SignedIn)
                {
                    ChoreSync.Start(this);
                    LookUpInvite();
                }
                else if (result == AccountManager.SignInResult.NoGoogleAccount)
                    ShortToast.Show(this, R.string.sign_in_no_google_account);
                else if (result == AccountManager.SignInResult.Failed)
                    ShortToast.Show(this, R.string.sign_in_failed);
            });
        });
    }

    private void LookUpInvite()
    {
        binding.joinTitle.setText(R.string.group_join_checking);
        binding.joinText.setText("");
        binding.joinButton.setVisibility(View.GONE);
        ShowBusy(true);
        GroupManager.LookUpInvite(Code, invite -> {
            ShowBusy(false);
            if (invite == null)
            {
                ShowInvalid();
                return;
            }
            Invite = invite;
            ShowConfirmation();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        GroupManager.AddListener(GroupChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GroupManager.RemoveListener(GroupChanged);
    }

    // The user's groups loaded or changed: whether they're already in the invite's group may be known now.
    private final GroupManager.OnGroupChanged GroupChanged = new GroupManager.OnGroupChanged() {
        @Override
        public void GroupChanged() {
            if (Invite != null && !Joining)
                ShowConfirmation();
        }
    };
    private boolean Joining = false;

    private void ShowConfirmation()
    {
        String groupName = Invite.GroupName == null ? "" : Invite.GroupName;
        if (GroupManager.GetGroup(Invite.GroupId) != null)
        {
            binding.joinTitle.setText(getString(R.string.group_join_already_title, groupName));
            binding.joinText.setText(R.string.group_join_already_text);
            binding.joinButton.setVisibility(View.GONE);
            return;
        }
        binding.joinTitle.setText(getString(R.string.group_join_title, groupName));
        binding.joinText.setText(R.string.group_join_text);
        binding.joinButton.setVisibility(View.VISIBLE);
        binding.joinButton.setText(R.string.group_join);
        binding.joinButton.setEnabled(GroupManager.IsLoaded());   // wait until the user's groups are known
        binding.joinButton.setOnClickListener(view -> Join());
    }

    private void Join()
    {
        Joining = true;
        ShowBusy(true);
        GroupManager.Join(Invite, success -> {
            Joining = false;
            ShowBusy(false);
            if (!success)
            {
                ShortToast.Show(this, R.string.group_action_failed);
                return;
            }
            ShortToast.Show(this, getString(R.string.group_joined, Invite.GroupName));
            // From a link: into the app. From "Join with a code": back to the group list.
            if (isTaskRoot())
                OpenApp();
            else
                finish();
        });
    }

    private void ShowInvalid()
    {
        binding.joinTitle.setText(R.string.group_join_invalid_title);
        binding.joinText.setText(R.string.group_join_invalid_text);
        binding.joinButton.setVisibility(View.GONE);
        binding.cancelButton.setText(R.string.group_close);
    }

    // After joining: the daily view, with the week view behind it, as when the app starts.
    private void OpenApp()
    {
        Intent week = new Intent(this, MainActivity.class);
        week.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivities(new Intent[] { week, DayActivity.CreateDailyViewIntent(this) });
        finish();
    }
}
