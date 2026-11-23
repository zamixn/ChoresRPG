package com.zsoftware.choresrpg;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.choreicon.ChoreIconPicker;
import com.zsoftware.choresrpg.databinding.ActivityChoreBinding;
import com.zsoftware.choresrpg.databinding.DialogEditDescriptionBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Shows all data of one chore. Every property has an edit button; the chore can also be marked as
// completed (once per day) or deleted. Opened by tapping a chore in the week view or the day view.
public class ChoreActivity extends AppCompatActivity {

    private static final String ExtraChoreId = "ChoreId";

    private static final int[] ShortDayNames = {
            R.string.week_day_button_monday,
            R.string.week_day_button_tuesday,
            R.string.week_day_button_wednesday,
            R.string.week_day_button_thursday,
            R.string.week_day_button_friday,
            R.string.week_day_button_saturday,
            R.string.week_day_button_sunday
    };
    private static final int[] FullDayNames = {
            R.string.week_day_button_monday_full,
            R.string.week_day_button_tuesday_full,
            R.string.week_day_button_wednesday_full,
            R.string.week_day_button_thursday_full,
            R.string.week_day_button_friday_full,
            R.string.week_day_button_saturday_full,
            R.string.week_day_button_sunday_full
    };

    private ActivityChoreBinding binding;
    private String ChoreId;
    private Chore CurrentChore;
    private ColorStateList DefaultCompleteButtonTint;

    public static Intent CreateIntent(Context context, Chore chore)
    {
        Intent intent = new Intent(context, ChoreActivity.class);
        intent.putExtra(ExtraChoreId, chore.GetId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreIconDictionary.Initialize(this);

        binding = ActivityChoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentChore);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // The title bar shows its own icon + title views (see activity_chore.xml).
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ChoreId = getIntent().getStringExtra(ExtraChoreId);
        DefaultCompleteButtonTint = binding.completeButton.getBackgroundTintList();

        binding.editIconButton.setOnClickListener(view -> ShowEditIconDialog());
        binding.resetLastCompletedButton.setOnClickListener(view -> ResetLastCompleted());
        binding.editDescriptionButton.setOnClickListener(view -> ShowEditDescriptionDialog());
        binding.editDaysButton.setOnClickListener(view -> ShowEditDaysDialog());
        binding.editLastCompletedButton.setOnClickListener(view -> ShowEditLastCompletedDialog());
        binding.completeButton.setOnClickListener(view -> CompleteChore());
        binding.deleteButton.setOnClickListener(view -> ShowDeleteDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        CurrentChore = SaveData.Instance().GetChore(ChoreId);
        if (CurrentChore == null)
        {
            ShortToast.Show(this, R.string.chore_not_found);
            finish();
            return;
        }
        ShowChore();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowChore()
    {
        Chore chore = CurrentChore;
        setTitle(chore.GetDescription());
        binding.toolbarTitle.setText(chore.GetDescription());
        binding.choreIcon.setImageDrawable(GetIconCopy(chore));

        WeekDay[] weekDays = WeekDay.values();
        List<String> scheduledDays = new ArrayList<>();
        binding.choreDays.removeAllViews();
        for (int i = 0; i < weekDays.length; i++)
        {
            boolean isScheduled = chore.IsOnDay(weekDays[i]);
            if (isScheduled)
                scheduledDays.add(getString(FullDayNames[i]));

            TextView day = (TextView) getLayoutInflater().inflate(R.layout.chore_week_day, binding.choreDays, false);
            day.setText(ShortDayNames[i]);
            day.setBackgroundResource(isScheduled ? R.drawable.round_button_selected : R.drawable.round_button);
            binding.choreDays.addView(day);
        }

        if (scheduledDays.isEmpty())
            binding.choreDaysSummary.setText(R.string.chore_no_days);
        else if (scheduledDays.size() == weekDays.length)
            binding.choreDaysSummary.setText(R.string.chore_every_day);
        else
            binding.choreDaysSummary.setText(String.join(", ", scheduledDays));

        Long lastCompleted = chore.GetLastCompleted();
        if (lastCompleted == null)
            binding.choreLastCompletedText.setText(R.string.chore_never_completed);
        else
            binding.choreLastCompletedText.setText(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(lastCompleted)));
        // Nothing to reset when the chore was never completed.
        binding.resetLastCompletedButton.setEnabled(lastCompleted != null);
        binding.resetLastCompletedButton.setAlpha(lastCompleted != null ? 1f : 0.3f);

        // Completed today: green button that only tells the user it's already done.
        if (chore.IsCompletedToday())
        {
            binding.completeButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.completed_green)));
            binding.completeButton.setText(R.string.completed_today);
        }
        else
        {
            binding.completeButton.setBackgroundTintList(DefaultCompleteButtonTint);
            binding.completeButton.setText(R.string.complete_chore);
        }
    }

    // Saves the edited chore and shows the new values.
    private void SaveChore()
    {
        SaveData.Instance().UpdateChore(CurrentChore);
        ShowChore();
    }

    private void CompleteChore()
    {
        if (CurrentChore.IsCompletedToday())
        {
            ShortToast.Show(this, R.string.chore_already_completed);
            return;
        }
        CurrentChore.Complete();
        SaveChore();
        ShortToast.Show(this, R.string.chore_completed);
    }

    private void ResetLastCompleted()
    {
        CurrentChore.SetLastCompleted(null);
        SaveChore();
        ShortToast.Show(this, R.string.last_completed_reset);
    }

    private void ShowEditDescriptionDialog()
    {
        DialogEditDescriptionBinding dialogBinding = DialogEditDescriptionBinding.inflate(getLayoutInflater());
        dialogBinding.descriptionInput.setText(CurrentChore.GetDescription());
        dialogBinding.descriptionInput.setSelection(CurrentChore.GetDescription().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_description)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();
        // Set after show() so an empty description keeps the dialog open instead of closing it.
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            String description = Chore.CleanDescription(String.valueOf(dialogBinding.descriptionInput.getText()));
            if (description.isEmpty())
            {
                dialogBinding.descriptionInput.setError(getString(R.string.description_empty));
                return;
            }
            CurrentChore.SetDescription(description);
            SaveChore();
            dialog.dismiss();
        });
    }

    private void ShowEditIconDialog()
    {
        ChoreIconPicker.Show(this, CurrentChore.GetIcon(), icon -> {
            CurrentChore.SetIcon(icon);
            SaveChore();
        });
    }

    private void ShowEditDaysDialog()
    {
        WeekDay[] weekDays = WeekDay.values();
        String[] dayNames = new String[weekDays.length];
        boolean[] checked = new boolean[weekDays.length];
        for (int i = 0; i < weekDays.length; i++)
        {
            dayNames[i] = getString(FullDayNames[i]);
            checked[i] = CurrentChore.IsOnDay(weekDays[i]);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_days)
                .setMultiChoiceItems(dayNames, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    List<WeekDay> selected = new ArrayList<>();
                    for (int i = 0; i < weekDays.length; i++)
                    {
                        if (checked[i])
                            selected.add(weekDays[i]);
                    }
                    CurrentChore.SetWeekDays(selected);
                    SaveChore();
                })
                .show();
    }

    // Picks the date of the last completion. The time of day is kept from the old value (or now).
    // Future dates can't be picked. (The reset button next to it clears the date.)
    private void ShowEditLastCompletedDialog()
    {
        Calendar current = Calendar.getInstance();
        if (CurrentChore.GetLastCompleted() != null)
            current.setTimeInMillis(CurrentChore.GetLastCompleted());

        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, dayOfMonth) -> {
            Calendar picked = (Calendar) current.clone();
            picked.set(year, month, dayOfMonth);
            long now = System.currentTimeMillis();
            CurrentChore.SetLastCompleted(Math.min(picked.getTimeInMillis(), now));
            SaveChore();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    // Confirmation popup: shows the chore's own icon and name so it's clear what will be removed,
    // with a red Delete button next to Cancel. Deleting returns to the previous screen.
    private void ShowDeleteDialog()
    {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setIcon(GetIconCopy(CurrentChore))
                .setTitle(R.string.delete_chore_title)
                .setMessage(getString(R.string.delete_chore_message, CurrentChore.GetDescription()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    SaveData.Instance().DeleteChore(CurrentChore.GetId());
                    ShortToast.Show(this, R.string.chore_deleted);
                    finish();
                })
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.delete_red));
    }

    // The icon dictionary's drawables are shared; each view or dialog gets its own copy.
    private Drawable GetIconCopy(Chore chore)
    {
        Drawable icon = ChoreIconDictionary.Instance().GetIcon(chore.GetIcon());
        if (icon == null || icon.getConstantState() == null)
            return icon;
        return icon.getConstantState().newDrawable(getResources()).mutate();
    }
}
