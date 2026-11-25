package com.zsoftware.choresrpg.group;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ScreenSetup;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.ActivityGroupListBinding;
import com.zsoftware.choresrpg.databinding.GroupListItemBinding;

// The user's groups (oldest first, the main group marked). Tapping one opens it (GroupActivity); below the
// list: create a new group, join one with a code. Opened from Settings; only while signed in.
public class GroupListActivity extends AppCompatActivity {

    private ActivityGroupListBinding binding;

    public static Intent CreateIntent(Context context)
    {
        return new Intent(context, GroupListActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);

        binding = ActivityGroupListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentGroupList);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.group_list_title);

        if (!AccountManager.IsSignedIn())
        {
            ShortToast.Show(this, R.string.group_sign_in_needed);
            finish();
            return;
        }

        binding.createGroupButton.setOnClickListener(view -> CreateNewGroup());
        binding.joinWithCodeButton.setOnClickListener(view ->
                GroupDialogs.AskInviteCode(this, code -> startActivity(JoinGroupActivity.CreateIntent(this, code))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Also tries loading the groups again if that failed before.
        ChoreSync.Start(this);
        ShowGroups();
        GroupManager.AddListener(GroupChanged);
        ScreenSetup.ShowRemovedFromGroupNotice(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GroupManager.RemoveListener(GroupChanged);
    }

    private final GroupManager.OnGroupChanged GroupChanged = new GroupManager.OnGroupChanged() {
        @Override
        public void GroupChanged() {
            ShowGroups();
            ScreenSetup.ShowRemovedFromGroupNotice(GroupListActivity.this);
        }
    };

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowGroups()
    {
        boolean loaded = GroupManager.IsLoaded();
        binding.loadingText.setVisibility(loaded ? View.GONE : View.VISIBLE);
        binding.loadingText.setText(GroupManager.HasLoadFailed() ? R.string.group_load_failed : R.string.group_loading);
        binding.createGroupButton.setEnabled(loaded);
        binding.joinWithCodeButton.setEnabled(loaded);

        binding.groupsList.removeAllViews();
        if (!loaded)
            return;
        String mainGroupId = GroupManager.GetMainGroupId();
        for (Group group : GroupManager.GetGroups())
        {
            GroupListItemBinding row = GroupListItemBinding.inflate(getLayoutInflater(), binding.groupsList, false);
            row.groupName.setText(group.Name);
            int members = group.Members.size();
            row.groupSummary.setText(getResources().getQuantityString(R.plurals.group_member_count, members, members));
            row.mainLabel.setVisibility(group.Id.equals(mainGroupId) ? View.VISIBLE : View.GONE);
            row.getRoot().setOnClickListener(view -> startActivity(GroupActivity.CreateIntent(this, group.Id)));
            binding.groupsList.addView(row.getRoot());
        }
    }

    // Asks for a name (and whether it becomes the main group), creates the group and opens it.
    private void CreateNewGroup()
    {
        GroupDialogs.CreateGroup(this, false, groupId -> startActivity(GroupActivity.CreateIntent(this, groupId)));
    }
}
