package com.zsoftware.choresrpg.choreicon;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;

// Popup with a scrollable grid of all chore icons. Used by the add-chore form and the chore view.
public class ChoreIconPicker {

    public interface OnIconPicked
    {
        void IconPicked(ChoreIconEnum icon);
    }

    // Fraction of the screen height the popup takes, so the grid has room to scroll.
    private static final float HeightOfScreen = 0.75f;

    public static void Show(Activity activity, ChoreIconEnum currentIcon, OnIconPicked onPicked)
    {
        GridView grid = (GridView) activity.getLayoutInflater().inflate(R.layout.chore_icon_selector_dialog, null);
        ChoreImageAdapter adapter = new ChoreImageAdapter(activity, currentIcon);
        grid.setAdapter(adapter);
        // Start with the current icon in view.
        grid.setSelection(adapter.GetPosition(currentIcon));

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.choose_icon)
                .setView(grid)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        grid.setOnItemClickListener((parent, view, position, id) -> {
            onPicked.IconPicked(adapter.getItem(position));
            dialog.dismiss();
        });
        dialog.show();

        if (dialog.getWindow() != null)
        {
            int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * HeightOfScreen);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
        }
    }
}
