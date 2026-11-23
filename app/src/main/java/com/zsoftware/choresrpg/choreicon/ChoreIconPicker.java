package com.zsoftware.choresrpg.choreicon;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.databinding.ChoreIconSelectorDialogBinding;

// Popup with a search field and a scrollable grid of chore icons. Typing filters the icons by their
// keyword tags (see ChoreIconEnum.MatchesSearch). Used by the add-chore form and the chore view.
public class ChoreIconPicker {

    public interface OnIconPicked
    {
        void IconPicked(ChoreIconEnum icon);
    }

    // Fraction of the screen height the popup takes, so the grid has room to scroll.
    private static final float HeightOfScreen = 0.75f;

    public static void Show(Activity activity, ChoreIconEnum currentIcon, OnIconPicked onPicked)
    {
        ChoreIconSelectorDialogBinding picker = ChoreIconSelectorDialogBinding.inflate(activity.getLayoutInflater());
        ChoreImageAdapter adapter = new ChoreImageAdapter(activity, currentIcon);
        picker.gridView.setAdapter(adapter);
        // Start with the current icon in view.
        picker.gridView.setSelection(adapter.GetPosition(currentIcon));

        picker.iconSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.SetSearchText(s.toString());
                picker.gridView.setSelection(0);
                picker.noIconsText.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
        // The keyboard's search key just hides the keyboard; the grid already shows the results.
        picker.iconSearch.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH)
                return false;
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (keyboard != null)
                keyboard.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            return true;
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.choose_icon)
                .setView(picker.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        picker.gridView.setOnItemClickListener((parent, view, position, id) -> {
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
