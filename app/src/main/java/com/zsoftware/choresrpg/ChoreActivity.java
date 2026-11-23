package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.databinding.ActivityChoreBinding;

import java.util.ArrayList;
import java.util.List;

// Shows all data of one chore: icon, description and the weekdays it is scheduled on.
// Opened by tapping a chore in the week view or the day view.
public class ChoreActivity extends AppCompatActivity {

    // Chores have no id yet, so the chore is passed in its save format (see Chore.ToSaveDataString).
    private static final String ExtraChore = "Chore";

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

    public static Intent CreateIntent(Context context, Chore chore)
    {
        Intent intent = new Intent(context, ChoreActivity.class);
        intent.putExtra(ExtraChore, chore.ToSaveDataString());
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Chore chore = Chore.Parse(getIntent().getStringExtra(ExtraChore));
        ShowChore(chore);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void ShowChore(Chore chore)
    {
        setTitle(chore.GetDescription());

        binding.choreIcon.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(chore.GetIcon()));
        binding.choreDescriptionText.setText(chore.GetDescription());
        binding.choreIconName.setText(getString(R.string.chore_icon_label, chore.GetIcon().name()));

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
    }
}
