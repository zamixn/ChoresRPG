package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.adapters.ChoreAdapter;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.databinding.ActivityDayBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Lists all chores of one day. Opened from the week or month view; the arrows move to the previous
// or next day.
public class DayActivity extends AppCompatActivity {

    private static final String ExtraWeekDay = "WeekDay";
    private static final String ExtraDateMillis = "DateMillis";
    private static final String StateDateMillis = "DateMillis";

    private ActivityDayBinding binding;
    // The day that is shown and its weekday (which chores are listed).
    private Calendar CurrentDate;
    private WeekDay Day;

    public static Intent CreateIntent(Context context, WeekDay weekDay, Calendar date)
    {
        Intent intent = new Intent(context, DayActivity.class);
        intent.putExtra(ExtraWeekDay, weekDay.name());
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

        CurrentDate = AppClock.Now();
        if (savedInstanceState != null && savedInstanceState.containsKey(StateDateMillis))
            CurrentDate.setTimeInMillis(savedInstanceState.getLong(StateDateMillis));
        else if (getIntent().hasExtra(ExtraDateMillis))
            CurrentDate.setTimeInMillis(getIntent().getLongExtra(ExtraDateMillis, 0));
        else if (getIntent().hasExtra(ExtraWeekDay))
            CurrentDate = WeekDay.valueOf(getIntent().getStringExtra(ExtraWeekDay)).GetDateThisWeek();
        Day = WeekDay.FromCalendar(CurrentDate);

        binding.dayChoresList.setEmptyView(binding.noChoresText);
        ScreenSetup.SetupPeriodNavigator(binding.periodNavigator, R.string.previous_day, R.string.next_day,
                () -> ShowDay(-1), () -> ShowDay(1), this::ShowToday);

        ScreenSetup.ApplyFabInsets(binding.addChoreFab);
        binding.addChoreFab.setOnClickListener(view ->
                startActivity(AddChoreActivity.CreateIntent(DayActivity.this, Day)));
    }

    // Reloads on every return, so chores edited or deleted in the chore view show up correctly.
    @Override
    protected void onResume() {
        super.onResume();
        ShowChores();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(StateDateMillis, CurrentDate.getTimeInMillis());
    }

    private void ShowDay(int daysToMove)
    {
        CurrentDate.add(Calendar.DAY_OF_MONTH, daysToMove);
        Day = WeekDay.FromCalendar(CurrentDate);
        ShowChores();
    }

    private void ShowToday()
    {
        CurrentDate = AppClock.Now();
        Day = WeekDay.FromCalendar(CurrentDate);
        ShowChores();
    }

    // Title, relative label and the list of the shown day. Chores completed on it get a green checkmark.
    private void ShowChores()
    {
        setTitle(GetDayTitle());
        binding.periodNavigator.periodLabel.setText(GetRelativeDayLabel());

        List<Chore> chores = SaveData.Instance().GetChoreList();
        ChoreAdapter adapter = new ChoreAdapter(this, chores, Day, CurrentDate);
        binding.dayChoresList.setAdapter(adapter);
        binding.dayChoresList.setOnItemClickListener((parent, view, position, id) ->
                startActivity(ChoreActivity.CreateIntent(DayActivity.this, adapter.getItem(position))));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // e.g. "Monday, 23 November" (with the year when it isn't this year).
    private String GetDayTitle()
    {
        boolean isThisYear = CurrentDate.get(Calendar.YEAR) == AppClock.Now().get(Calendar.YEAR);
        String pattern = isThisYear ? "d MMMM" : "d MMMM yyyy";
        String dayAndMonth = new SimpleDateFormat(pattern, Locale.getDefault()).format(CurrentDate.getTime());
        return getString(GetFullDayName(Day)) + ", " + dayAndMonth;
    }

    // "Today", "Tomorrow", "Yesterday", "In 3 days" or "3 days ago".
    private String GetRelativeDayLabel()
    {
        int days = DaysFromToday(CurrentDate);
        if (days == 0)
            return getString(R.string.today);
        if (days == 1)
            return getString(R.string.tomorrow);
        if (days == -1)
            return getString(R.string.yesterday);
        if (days > 0)
            return getResources().getQuantityString(R.plurals.days_from_now, days, days);
        return getResources().getQuantityString(R.plurals.days_ago, -days, -days);
    }

    // Whole calendar days between today and the date (negative in the past).
    private static int DaysFromToday(Calendar date)
    {
        Calendar today = StartOfDay(AppClock.Now());
        Calendar day = StartOfDay((Calendar) date.clone());
        // Rounded, because a daylight saving change makes a day 23 or 25 hours long.
        return (int) Math.round((day.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
    }

    private static Calendar StartOfDay(Calendar date)
    {
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
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
