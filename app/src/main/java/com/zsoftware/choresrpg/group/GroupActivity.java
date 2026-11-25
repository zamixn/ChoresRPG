package com.zsoftware.choresrpg.group;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.ActivityGroupBinding;
import com.zsoftware.choresrpg.databinding.GroupMemberItemBinding;

// The user's group: its name (editable), creator and members, plus invite / join with a code /
// create a new group / leave.
// The creator can remove members. Opened from Settings; only while signed in.
public class GroupActivity extends AppCompatActivity {

    private ActivityGroupBinding binding;

    public static Intent CreateIntent(Context context)
    {
        return new Intent(context, GroupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);

        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentGroup);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.group_title);

        if (!AccountManager.IsSignedIn())
        {
            ShortToast.Show(this, R.string.group_sign_in_needed);
            finish();
            return;
        }

        binding.editNameButton.setOnClickListener(view -> Rename());
        binding.inviteButton.setOnClickListener(view -> Invite());
        binding.joinWithCodeButton.setOnClickListener(view -> JoinWithCode());
        binding.createGroupButton.setOnClickListener(view -> CreateNewGroup());
        binding.leaveButton.setOnClickListener(view -> Leave());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ShowGroup();
        GroupManager.AddListener(GroupChanged);
        ScreenSetup.ShowRemovedFromGroupNotice(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GroupManager.RemoveListener(GroupChanged);
    }

    // The group changed (renamed, members joined / left, or this user was moved to a new group): redraw.
    private final GroupManager.OnGroupChanged GroupChanged = new GroupManager.OnGroupChanged() {
        @Override
        public void GroupChanged() {
            ShowGroup();
            ScreenSetup.ShowRemovedFromGroupNotice(GroupActivity.this);
        }
    };

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowGroup()
    {
        Group group = GroupManager.GetCurrentGroup();
        binding.loadingText.setVisibility(group == null ? View.VISIBLE : View.GONE);
        binding.groupDetails.setVisibility(group == null ? View.GONE : View.VISIBLE);
        SetButtonsEnabled(true);
        if (group == null)
            return;

        String userId = AccountManager.GetUserId();
        boolean isCreator = group.OwnerId.equals(userId);
        binding.groupName.setText(group.Name);
        binding.groupCreator.setText(getString(R.string.group_created_by, group.GetMemberName(group.OwnerId)));

        binding.membersList.removeAllViews();
        for (String memberId : group.Members)
        {
            GroupMemberItemBinding row = GroupMemberItemBinding.inflate(getLayoutInflater(), binding.membersList, false);
            String name = group.GetMemberName(memberId);
            row.memberName.setText(memberId.equals(userId) ? getString(R.string.group_member_you, name) : name);
            row.memberRole.setVisibility(memberId.equals(group.OwnerId) ? View.VISIBLE : View.GONE);
            // The creator can remove anyone but themselves.
            boolean canRemove = isCreator && !memberId.equals(userId);
            row.removeButton.setVisibility(canRemove ? View.VISIBLE : View.GONE);
            row.removeButton.setContentDescription(getString(R.string.group_remove_member_named, name));
            row.removeButton.setOnClickListener(view -> RemoveMember(memberId, name));
            binding.membersList.addView(row.getRoot());
        }

        // Alone in the group: nothing to leave (inviting people is the way to share).
        boolean alone = group.Members.size() <= 1;
        binding.onlyMemberText.setVisibility(alone ? View.VISIBLE : View.GONE);
        binding.leaveButton.setVisibility(alone ? View.GONE : View.VISIBLE);
    }

    private void SetButtonsEnabled(boolean enabled)
    {
        binding.editNameButton.setEnabled(enabled);
        binding.inviteButton.setEnabled(enabled);
        binding.joinWithCodeButton.setEnabled(enabled);
        binding.createGroupButton.setEnabled(enabled);
        binding.leaveButton.setEnabled(enabled);
    }

    private void Rename()
    {
        Group group = GroupManager.GetCurrentGroup();
        if (group == null)
            return;
        GroupDialogs.AskName(this, R.string.group_rename, group.Name, name ->
                GroupManager.Rename(name, success -> {
                    if (!success)
                        ShortToast.Show(this, R.string.group_action_failed);
                    ShowGroup();
                }));
    }

    // Shares an invite (any app: messages, mail ...) with both the app link and the code. Some apps don't
    // make choresrpg:// links tappable, so the code can be typed in with "Join with a code" instead.
    private void Invite()
    {
        Group group = GroupManager.GetCurrentGroup();
        if (group == null)
            return;
        SetButtonsEnabled(false);
        GroupManager.GetInviteCode(code -> {
            SetButtonsEnabled(true);
            if (code == null)
            {
                ShortToast.Show(this, R.string.group_action_failed);
                return;
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.group_invite_message, group.Name,
                    GroupManager.GetInviteLink(code), code));
            startActivity(Intent.createChooser(share, getString(R.string.group_invite)));
        });
    }

    // Asks for an invite code (or a pasted invite link) and opens the join screen for it.
    private void JoinWithCode()
    {
        GroupDialogs.AskInviteCode(this, code -> startActivity(JoinGroupActivity.CreateIntent(this, code)));
    }

    private void RemoveMember(String memberId, String name)
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.group_remove_title, name))
                .setMessage(R.string.group_remove_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.group_remove, (d, which) ->
                        GroupManager.RemoveMember(memberId, success -> {
                            if (!success)
                                ShortToast.Show(this, R.string.group_action_failed);
                        }))
                .show();
    }

    // New group with a name; then: take this phone's chores into it, or start empty?
    private void CreateNewGroup()
    {
        Group group = GroupManager.GetCurrentGroup();
        if (group == null)
            return;
        GroupDialogs.AskName(this, R.string.group_create_new, null, name ->
                GroupDialogs.AskAboutChores(this, R.string.group_create_chores_title,
                        getString(R.string.group_create_chores_message, group.Name),
                        R.string.group_bring_chores, R.string.group_start_empty,
                        bring -> {
                            SetButtonsEnabled(false);
                            GroupManager.CreateNewGroup(name, bring, success -> FinishedSwitching(success, R.string.group_created));
                        }));
    }

    // Leave: explains what happens (and who becomes the creator), then: keep a copy of the chores?
    private void Leave()
    {
        Group group = GroupManager.GetCurrentGroup();
        if (group == null)
            return;
        String userId = AccountManager.GetUserId();
        String message = getString(R.string.group_leave_message, group.Name);
        String newOwner = group.GetOwnerAfterLeaving(userId);
        if (group.OwnerId.equals(userId) && newOwner != null)
            message += " " + getString(R.string.group_leave_new_owner, group.GetMemberName(newOwner));
        GroupDialogs.AskAboutChores(this, R.string.group_leave_title, message,
                R.string.group_keep_copy, R.string.group_start_empty,
                keep -> {
                    SetButtonsEnabled(false);
                    GroupManager.Leave(keep, success -> FinishedSwitching(success, R.string.group_left));
                });
    }

    private void FinishedSwitching(boolean success, int doneMessageId)
    {
        ShortToast.Show(this, success ? doneMessageId : R.string.group_action_failed);
        // The new group is being looked up; the listener redraws when it's there.
        ShowGroup();
    }
}
