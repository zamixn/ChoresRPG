package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.choreicon.ChoreIconPicker;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.databinding.ActivityChoreBinding;
import com.zsoftware.choresrpg.databinding.DialogEditDescriptionBinding;
import com.zsoftware.choresrpg.schedule.ScheduleEditor;
import com.zsoftware.choresrpg.schedule.ScheduleText;

import java.text.DateFormat;
import java.util.Date;

// Shows all data of one chore. The description, icon and days have edit buttons; the chore can also be
// marked as completed today (tap again to undo) or deleted, and its completions are listed (and can be
// deleted) in the history. Opened by tapping a chore in the week view or the day view.
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
        ChoreSync.Start(this);
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
        binding.editDescriptionButton.setOnClickListener(view -> ShowEditDescriptionDialog());
        binding.editDaysButton.setOnClickListener(view -> ShowEditDaysDialog());
        binding.completeButton.setOnClickListener(view -> CompleteChore());
        binding.deleteButton.setOnClickListener(view -> ShowDeleteDialog());
        binding.viewHistoryButton.setOnClickListener(view ->
                startActivity(ChoreHistoryActivity.CreateIntent(ChoreActivity.this, CurrentChore)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadChore();
        ChoreSync.AddListener(CloudChoresChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChoreSync.RemoveListener(CloudChoresChanged);
    }

    // Chores changed on another device: show this chore's new data (or close if it was deleted).
    private final ChoreSync.OnChoresChanged CloudChoresChanged = new ChoreSync.OnChoresChanged() {
        @Override
        public void ChoresChanged() {
            LoadChore();
        }
    };

    private void LoadChore()
    {
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

        // Weekly: a circle per weekday (filled on its days). Monthly: just the description.
        ChoreSchedule schedule = chore.GetSchedule();
        boolean weekly = schedule.GetRepeat() == ChoreSchedule.RepeatKind.Weekly;
        WeekDay[] weekDays = WeekDay.values();
        binding.choreDays.removeAllViews();
        binding.choreDays.setVisibility(weekly ? View.VISIBLE : View.GONE);
        for (int i = 0; weekly && i < weekDays.length; i++)
        {
            TextView day = (TextView) getLayoutInflater().inflate(R.layout.chore_week_day, binding.choreDays, false);
            day.setText(ShortDayNames[i]);
            day.setSelected(chore.IsOnDay(weekDays[i]));
            binding.choreDays.addView(day);
        }
        binding.choreDaysSummary.setText(ScheduleText.Describe(this, schedule));

        Long lastCompleted = chore.GetLastCompleted();
        if (lastCompleted == null)
            binding.choreLastCompletedText.setText(R.string.chore_never_completed);
        else
            binding.choreLastCompletedText.setText(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(lastCompleted)));

        // The group it's shared in (known once signed in and the groups are loaded).
        Group group = GroupManager.GetGroup(chore.GetGroupId());
        binding.choreGroupRow.setVisibility(group != null ? View.VISIBLE : View.GONE);
        if (group != null)
            binding.choreGroupText.setText(group.Name);

        // Completed today: green button; tapping it again undoes today's completion.
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

    // The complete button toggles today: completes the chore, or (green "Completed today") undoes today's
    // completion. Older completions are managed in the chore history.
    private void CompleteChore()
    {
        if (CurrentChore.IsCompletedToday())
        {
            CurrentChore.RemoveCompletionsOn(AppClock.Now());
            SaveChore();
            ShortToast.Show(this, R.string.chore_uncompleted);
            return;
        }
        CurrentChore.Complete();
        SaveChore();
        ShortToast.Show(this, R.string.chore_completed);
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

    // The schedule editor (weekly / monthly, every n) in a popup; Save applies it.
    private void ShowEditDaysDialog()
    {
        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_content_padding);
        container.setPadding(padding, padding, padding, 0);
        scroll.addView(container);
        ScheduleEditor editor = new ScheduleEditor(this, getLayoutInflater(), container, CurrentChore.GetSchedule());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_days)
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    CurrentChore.SetSchedule(editor.GetSchedule());
                    SaveChore();
                })
                .show();
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
