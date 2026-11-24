package com.zsoftware.choresrpg;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.settings.SettingsActivity;
import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.databinding.ActivityMonthBinding;
import com.zsoftware.choresrpg.databinding.CalendarDayCellBinding;
import com.zsoftware.choresrpg.databinding.CalendarDayChoreBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Monthly view: a calendar of one month (the current one at first; arrows move between months) with
// each day's chores listed in its cell.
public class MonthActivity extends AppCompatActivity {

    private static final int DaysInWeek = 7;
    private static final int[] WeekDayHeaderStrings = {
            R.string.week_day_button_monday,
            R.string.week_day_button_tuesday,
            R.string.week_day_button_wednesday,
            R.string.week_day_button_thursday,
            R.string.week_day_button_friday,
            R.string.week_day_button_saturday,
            R.string.week_day_button_sunday
    };

    private static final String StateDisplayedMonth = "DisplayedMonth";

    private ActivityMonthBinding binding;
    // First day of the month that is shown; the arrows move it one month back or forward.
    private Calendar DisplayedMonth;
    // Set when the shown month changes (and when the view opens): the next build scrolls to today in
    // the current month, or to the top-left corner in other months.
    private boolean ScrollPending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);
        ChoreIconDictionary.Initialize(this);

        binding = ActivityMonthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentMonth);

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.monthly_view);

        DisplayedMonth = GetFirstOfMonth(AppClock.Now());
        if (savedInstanceState != null && savedInstanceState.containsKey(StateDisplayedMonth))
            DisplayedMonth.setTimeInMillis(savedInstanceState.getLong(StateDisplayedMonth));
        ScreenSetup.SetupPeriodNavigator(binding.periodNavigator, R.string.previous_month, R.string.next_month,
                () -> ShowMonth(-1), () -> ShowMonth(1), this::ShowCurrentMonth);

        ScreenSetup.ApplyFabInsets(binding.addChoreFab);
        binding.addChoreFab.setOnClickListener(view ->
                startActivity(AddChoreActivity.CreateIntent(MonthActivity.this)));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(StateDisplayedMonth, DisplayedMonth.getTimeInMillis());
    }

    private void ShowMonth(int monthsToMove)
    {
        DisplayedMonth.add(Calendar.MONTH, monthsToMove);
        ScrollPending = true;
        BuildCalendar();
    }

    private void ShowCurrentMonth()
    {
        DisplayedMonth = GetFirstOfMonth(AppClock.Now());
        ScrollPending = true;
        BuildCalendar();
    }

    private static Calendar GetFirstOfMonth(Calendar date)
    {
        Calendar first = (Calendar) date.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        return first;
    }

    // Rebuilt on every return, so chores edited or deleted in the day or chore view show up correctly.
    @Override
    protected void onResume() {
        super.onResume();
        BuildCalendar();
        ChoreSync.AddListener(CloudChoresChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChoreSync.RemoveListener(CloudChoresChanged);
    }

    // Chores changed on another device: redraw.
    private final ChoreSync.OnChoresChanged CloudChoresChanged = new ChoreSync.OnChoresChanged() {
        @Override
        public void ChoresChanged() {
            BuildCalendar();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        ScreenSetup.MarkCurrentView(menu, R.id.action_monthly_view);
        ScreenSetup.SetupDebugMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // View buttons, Settings, Delete All Save Data and debug options (shared with the other views).
        if (ScreenSetup.HandleHeaderMenuItem(this, item.getItemId(), this::BuildCalendar))
            return true;
        return super.onOptionsItemSelected(item);
    }

    private void BuildCalendar()
    {
        LinearLayout calendar = binding.calendar;
        calendar.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        Calendar month = (Calendar) DisplayedMonth.clone();
        Calendar now = AppClock.Now();
        boolean isCurrentMonth = now.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == month.get(Calendar.MONTH);
        // Day of the month to highlight as today; -1 when another month is shown.
        int today = isCurrentMonth ? now.get(Calendar.DAY_OF_MONTH) : -1;
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstColumn = ToWeekDay(month.get(Calendar.DAY_OF_WEEK)).ordinal();

        binding.periodNavigator.periodLabel.setText(GetMonthTitle(month));

        // Weekday names live in their own row above the vertical scroll, so they stay on top.
        LinearLayout header = binding.calendarHeader;
        header.removeAllViews();
        for (int column = 0; column < DaysInWeek; column++)
        {
            TextView dayName = (TextView) inflater.inflate(R.layout.calendar_week_day_header, header, false);
            dayName.setText(WeekDayHeaderStrings[column]);
            header.addView(dayName, NewCellLayoutParams());
        }

        List<Chore> chores = SaveData.Instance().GetChoreList();
        int rowCount = (firstColumn + daysInMonth + DaysInWeek - 1) / DaysInWeek;
        View todayCell = null;
        for (int row = 0; row < rowCount; row++)
        {
            LinearLayout week = NewWeekRow();
            for (int column = 0; column < DaysInWeek; column++)
            {
                int dayOfMonth = row * DaysInWeek + column - firstColumn + 1;
                View cell;
                if (dayOfMonth < 1 || dayOfMonth > daysInMonth)
                    cell = new View(this);
                else
                    cell = CreateDayCell(inflater, week, month, dayOfMonth, WeekDay.values()[column], chores, dayOfMonth == today);
                week.addView(cell, NewCellLayoutParams());
                if (dayOfMonth == today)
                    todayCell = cell;
            }
            calendar.addView(week);
        }

        // When the view opens or another month is picked: center on today in the current month,
        // start at the top-left in other months. Not on other rebuilds, so coming back from a day keeps
        // the scroll position.
        if (ScrollPending)
        {
            ScrollPending = false;
            View cellToCenter = todayCell;
            calendar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    calendar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (cellToCenter != null)
                        CenterOn(cellToCenter);
                    else
                    {
                        binding.calendarHorizontalScroll.scrollTo(0, 0);
                        binding.calendarScroll.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    // Scrolls so the cell is in the middle of the screen, as far as the calendar's edges allow
    // (scrollTo stops at the first and last column/week).
    private void CenterOn(View cell)
    {
        Rect cellRect = new Rect(0, 0, cell.getWidth(), cell.getHeight());
        binding.calendar.offsetDescendantRectToMyCoords(cell, cellRect);
        binding.calendarHorizontalScroll.scrollTo(cellRect.centerX() - binding.calendarHorizontalScroll.getWidth() / 2, 0);
        binding.calendarScroll.scrollTo(0, cellRect.centerY() - binding.calendarScroll.getHeight() / 2);
    }

    private View CreateDayCell(LayoutInflater inflater, ViewGroup parent, Calendar month, int dayOfMonth, WeekDay weekDay, List<Chore> chores, boolean isToday)
    {
        CalendarDayCellBinding cell = CalendarDayCellBinding.inflate(inflater, parent, false);
        cell.dayNumber.setText(String.valueOf(dayOfMonth));
        if (isToday)
            cell.getRoot().setBackgroundResource(R.drawable.calendar_day_today);

        Calendar date = (Calendar) month.clone();
        date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cell.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(DayActivity.CreateIntent(MonthActivity.this, weekDay, date));
            }
        });

        for (Chore chore : chores)
        {
            if (!chore.IsOnDay(weekDay))
                continue;
            cell.dayChores.addView(CreateChoreRow(inflater, cell.dayChores, chore, chore.IsCompletedOn(date)));
        }
        return cell.getRoot();
    }

    // The chore's icon and name, plus a green checkmark (and green name) when it was completed that day,
    // like the week and day views.
    private View CreateChoreRow(LayoutInflater inflater, ViewGroup parent, Chore chore, boolean isCompleted)
    {
        CalendarDayChoreBinding row = CalendarDayChoreBinding.inflate(inflater, parent, false);
        row.choreName.setText(chore.GetDescription());

        Drawable icon = chore.GetIcon() == null ? null : ChoreIconDictionary.Instance().GetIcon(chore.GetIcon());
        // Each cell needs its own copy: the dictionary's drawables are shared between views.
        if (icon != null && icon.getConstantState() != null)
            icon = icon.getConstantState().newDrawable(getResources()).mutate();
        row.choreIcon.setImageDrawable(icon);

        row.completedCheck.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
        if (isCompleted)
            row.choreName.setTextColor(ContextCompat.getColor(this, R.color.completed_green));
        return row.getRoot();
    }

    private LinearLayout NewWeekRow()
    {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // Seven fixed-width columns (wider than most screens, so the calendar scrolls sideways);
    // cells stretch to the tallest cell in the week so the borders line up.
    private LinearLayout.LayoutParams NewCellLayoutParams()
    {
        int width = getResources().getDimensionPixelSize(R.dimen.calendar_day_width);
        return new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.MATCH_PARENT);
    }

    // Calendar.DAY_OF_WEEK runs Sunday (1) .. Saturday (7); WeekDay runs Monday .. Sunday.
    private static WeekDay ToWeekDay(int calendarDayOfWeek)
    {
        return WeekDay.values()[(calendarDayOfWeek + 5) % DaysInWeek];
    }

    private static String GetMonthTitle(Calendar month)
    {
        String title = new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(month.getTime());
        if (title.isEmpty())
            return title;
        return title.substring(0, 1).toUpperCase(Locale.getDefault()) + title.substring(1);
    }
}
