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

// One of the user's groups: its name (editable), creator and members, plus invite / leave. The creator
// can remove members. Opened from the group list; closes itself when the user is no longer in the group.
public class GroupActivity extends AppCompatActivity {

    private static final String ExtraGroupId = "GroupId";

    private ActivityGroupBinding binding;
    private String GroupId;

    public static Intent CreateIntent(Context context, String groupId)
    {
        Intent intent = new Intent(context, GroupActivity.class);
        intent.putExtra(ExtraGroupId, groupId);
        return intent;
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

        GroupId = getIntent().getStringExtra(ExtraGroupId);
        if (!AccountManager.IsSignedIn() || GroupId == null)
        {
            ShortToast.Show(this, R.string.group_sign_in_needed);
            finish();
            return;
        }

        binding.editNameButton.setOnClickListener(view -> Rename());
        binding.inviteButton.setOnClickListener(view -> Invite());
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

    // The group changed (renamed, members joined / left, or this user left / was removed): redraw.
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

    private Group GetGroup()
    {
        return GroupManager.GetGroup(GroupId);
    }

    private void ShowGroup()
    {
        Group group = GetGroup();
        if (group == null && GroupManager.IsLoaded())
        {
            // Left, removed, or deleted: nothing to show.
            finish();
            return;
        }
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

        boolean alone = group.Members.size() <= 1;
        binding.onlyMemberText.setVisibility(alone ? View.VISIBLE : View.GONE);
        // Alone in the only group: leaving would just make a new empty group; inviting people is the way to share.
        boolean onlyGroup = GroupManager.GetGroups().size() <= 1;
        binding.leaveButton.setVisibility(alone && onlyGroup ? View.GONE : View.VISIBLE);
    }

    private void SetButtonsEnabled(boolean enabled)
    {
        binding.editNameButton.setEnabled(enabled);
        binding.inviteButton.setEnabled(enabled);
        binding.leaveButton.setEnabled(enabled);
    }

    private void Rename()
    {
        Group group = GetGroup();
        if (group == null)
            return;
        GroupDialogs.AskName(this, R.string.group_rename, group.Name, name ->
                GroupManager.Rename(GroupId, name, success -> {
                    if (!success)
                        ShortToast.Show(this, R.string.group_action_failed);
                    ShowGroup();
                }));
    }

    // Asks whether to share the invite link or just the code. Some apps don't make choresrpg:// links
    // tappable; a code can always be typed in with "Join with a code".
    private void Invite()
    {
        if (GetGroup() == null)
            return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.group_invite)
                .setMessage(R.string.group_invite_choice_message)
                .setPositiveButton(R.string.group_invite_share_link, (dialog, which) -> ShareInvite(true))
                .setNegativeButton(R.string.group_invite_share_code, (dialog, which) -> ShareInvite(false))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    // Shares the invite (any app: messages, mail ...): the link that opens the app, or only the code.
    private void ShareInvite(boolean asLink)
    {
        Group group = GetGroup();
        if (group == null)
            return;
        SetButtonsEnabled(false);
        GroupManager.GetInviteCode(GroupId, code -> {
            SetButtonsEnabled(true);
            if (code == null)
            {
                ShortToast.Show(this, R.string.group_action_failed);
                return;
            }
            String message = asLink
                    ? getString(R.string.group_invite_link_message, group.Name, GroupManager.GetInviteLink(code))
                    : getString(R.string.group_invite_code_message, group.Name, code);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, getString(R.string.group_invite)));
        });
    }

    private void RemoveMember(String memberId, String name)
    {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.group_remove_title, name))
                .setMessage(R.string.group_remove_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.group_remove, (d, which) ->
                        GroupManager.RemoveMember(GroupId, memberId, success -> {
                            if (!success)
                                ShortToast.Show(this, R.string.group_action_failed);
                        }))
                .show();
    }

    // Leave: explains what happens (where a copy of the chores would go, who becomes the creator, or that
    // the group is deleted), then: keep a copy of its chores, or drop them?
    private void Leave()
    {
        Group group = GetGroup();
        if (group == null)
            return;
        String userId = AccountManager.GetUserId();
        Group copyTarget = GroupManager.GetLeaveCopyTarget(GroupId);
        String message = copyTarget == null
                ? getString(R.string.group_leave_message_only_group)
                : getString(R.string.group_leave_message, copyTarget.Name);
        String newOwner = group.GetOwnerAfterLeaving(userId);
        if (newOwner == null)
            message += " " + getString(R.string.group_leave_last_member);
        else if (group.OwnerId.equals(userId))
            message += " " + getString(R.string.group_leave_new_owner, group.GetMemberName(newOwner));
        GroupDialogs.AskAboutChores(this, getString(R.string.group_leave_title, group.Name), message,
                R.string.group_keep_copy, R.string.group_drop_chores,
                keep -> {
                    SetButtonsEnabled(false);
                    GroupManager.Leave(GroupId, keep, success -> {
                        ShortToast.Show(this, success ? R.string.group_left : R.string.group_action_failed);
                        if (success)
                            finish();
                        else
                            ShowGroup();
                    });
                });
    }
}
