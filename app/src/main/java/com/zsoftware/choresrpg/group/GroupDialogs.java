package com.zsoftware.choresrpg.group;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.DialogGroupNameBinding;
import com.zsoftware.choresrpg.databinding.DialogInviteCodeBinding;

import java.util.List;

// Popups used by the group screens.
public class GroupDialogs {

    public interface OnName
    {
        void Name(String name);
    }

    public interface OnChoice
    {
        void Chose(boolean yes);
    }

    public interface OnCode
    {
        void Code(String code);
    }

    public interface OnGroupChosen
    {
        void GroupChosen(String groupId);
    }

    // Asks which of the user's groups to use (a list with the current choice ticked). Picking one closes it.
    public static void ChooseGroup(Activity activity, int titleId, String selectedGroupId, OnGroupChosen onChosen)
    {
        List<Group> groups = GroupManager.GetGroups();
        if (groups.isEmpty())
            return;
        String[] names = new String[groups.size()];
        int selected = -1;
        for (int i = 0; i < groups.size(); i++)
        {
            names[i] = groups.get(i).Name;
            if (groups.get(i).Id.equals(selectedGroupId))
                selected = i;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(titleId)
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    dialog.dismiss();
                    onChosen.GroupChosen(groups.get(which).Id);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // Asks for an invite code (a pasted invite link works too). Unusable input keeps the popup open.
    public static void AskInviteCode(Activity activity, OnCode onCode)
    {
        DialogInviteCodeBinding input = DialogInviteCodeBinding.inflate(activity.getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.group_join_with_code)
                .setMessage(R.string.group_code_message)
                .setView(input.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.group_code_continue, null)
                .show();
        // Set after show() so a bad code keeps the popup open.
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            String code = GroupManager.ParseInviteCode(String.valueOf(input.codeInput.getText()));
            if (code == null)
            {
                input.codeInput.setError(activity.getString(R.string.group_code_empty));
                return;
            }
            dialog.dismiss();
            onCode.Code(code);
        });
    }

    // Asks for a group name (rename / new group). Empty names keep the popup open.
    public static void AskName(Activity activity, int titleId, String startName, OnName onName)
    {
        DialogGroupNameBinding input = DialogGroupNameBinding.inflate(activity.getLayoutInflater());
        if (startName != null)
        {
            input.nameInput.setText(startName);
            input.nameInput.setSelection(startName.length());
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(titleId)
                .setView(input.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();
        // Set after show() so an empty name keeps the popup open.
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = String.valueOf(input.nameInput.getText()).trim();
            if (name.isEmpty())
            {
                input.nameInput.setError(activity.getString(R.string.group_name_empty));
                return;
            }
            dialog.dismiss();
            onName.Name(name);
        });
    }

    // Asks what happens to a group's chores (e.g. when leaving it): a short explanation and two answers
    // (yes = keep a copy). Cancel does nothing.
    public static void AskAboutChores(Activity activity, String title, String message, int yesId, int noId, OnChoice onChoice)
    {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(yesId, (d, which) -> onChoice.Chose(true))
                .setNegativeButton(noId, (d, which) -> onChoice.Chose(false))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }
}
