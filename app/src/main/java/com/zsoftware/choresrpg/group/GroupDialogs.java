package com.zsoftware.choresrpg.group;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.DialogGroupNameBinding;
import com.zsoftware.choresrpg.databinding.DialogInviteCodeBinding;

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

    // Asks what happens to the chores on this phone when moving to another group: a short explanation and
    // two answers (yes = take them along / keep a copy). Cancel does nothing.
    public static void AskAboutChores(Activity activity, int titleId, String message, int yesId, int noId, OnChoice onChoice)
    {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(titleId)
                .setMessage(message)
                .setPositiveButton(yesId, (d, which) -> onChoice.Chose(true))
                .setNegativeButton(noId, (d, which) -> onChoice.Chose(false))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }
}
