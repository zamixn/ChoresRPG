package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.adapters.ChoreAdapter;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.databinding.ActivityDayBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Lists all chores of one day. Opened from the week view (a weekday) or the month view (a date).
public class DayActivity extends AppCompatActivity {

    private static final String ExtraWeekDay = "WeekDay";
    private static final String ExtraDateMillis = "DateMillis";

    private ActivityDayBinding binding;
    private WeekDay Day;

    // For the week view: the day is only known as a weekday.
    public static Intent CreateIntent(Context context, WeekDay weekDay)
    {
        Intent intent = new Intent(context, DayActivity.class);
        intent.putExtra(ExtraWeekDay, weekDay.name());
        return intent;
    }

    // For the month view: a specific date.
    public static Intent CreateIntent(Context context, WeekDay weekDay, Calendar date)
    {
        Intent intent = CreateIntent(context, weekDay);
        intent.putExtra(ExtraDateMillis, date.getTimeInMillis());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreIconDictionary.Initialize(this);

        binding = ActivityDayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentDay);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Day = WeekDay.valueOf(getIntent().getStringExtra(ExtraWeekDay));
        setTitle(GetDayTitle(Day));
        binding.dayChoresList.setEmptyView(binding.noChoresText);

        ScreenSetup.ApplyFabInsets(binding.addChoreFab);
        binding.addChoreFab.setOnClickListener(view ->
                startActivity(AddChoreActivity.CreateIntent(DayActivity.this, Day)));
    }

    // Reloads on every return, so chores edited or deleted in the chore view show up correctly.
    @Override
    protected void onResume() {
        super.onResume();
        List<Chore> chores = SaveData.Instance().GetChoreList();
        ChoreAdapter adapter = new ChoreAdapter(this, chores, Day, GetDate());
        binding.dayChoresList.setAdapter(adapter);
        binding.dayChoresList.setOnItemClickListener((parent, view, position, id) ->
                startActivity(ChoreActivity.CreateIntent(DayActivity.this, adapter.getItem(position))));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // e.g. "Monday, 23 November". From the week view the date is that weekday's date this week.
    private String GetDayTitle(WeekDay weekDay)
    {
        String dayAndMonth = new SimpleDateFormat("d MMMM", Locale.getDefault()).format(GetDate().getTime());
        return getString(GetFullDayName(weekDay)) + ", " + dayAndMonth;
    }

    // The date this list is for: the date from the month view, or this week's date of the weekday
    // from the week view. Chores completed on it get a green checkmark.
    private Calendar GetDate()
    {
        if (!getIntent().hasExtra(ExtraDateMillis))
            return Day.GetDateThisWeek();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(getIntent().getLongExtra(ExtraDateMillis, 0));
        return date;
    }

    private static int GetFullDayName(WeekDay weekDay)
    {
        switch (weekDay)
        {
            case Monday: return R.string.week_day_button_monday_full;
            case Tuesday: return R.string.week_day_button_tuesday_full;
            case Wednesday: return R.string.week_day_button_wednesday_full;
            case Thursday: return R.string.week_day_button_thursday_full;
            case Friday: return R.string.week_day_button_friday_full;
            case Saturday: return R.string.week_day_button_saturday_full;
            default: return R.string.week_day_button_sunday_full;
        }
    }
}
