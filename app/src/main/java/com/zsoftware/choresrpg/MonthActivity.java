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
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.databinding.ActivityMonthBinding;
import com.zsoftware.choresrpg.databinding.CalendarDayCellBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Monthly view: a calendar of the current month with each day's chores listed in its cell.
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

    private ActivityMonthBinding binding;
    private boolean ScrolledToToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreIconDictionary.Initialize(this);

        binding = ActivityMonthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentMonth);

        setSupportActionBar(binding.toolbar);
    }

    // Rebuilt on every return, so chores edited or deleted in the day or chore view show up correctly.
    @Override
    protected void onResume() {
        super.onResume();
        BuildCalendar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        ScreenSetup.MarkCurrentView(menu, R.id.action_monthly_view);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if(id == R.id.action_weekly_view)
        {
            OpenWeeklyView();
            return true;
        }

        if(id == R.id.action_delete_all_save_data)
        {
            SaveData.Instance().DeleteAll();
            ShortToast.Show(this, R.string.all_data_deleted);
            BuildCalendar();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // The weekly view (MainActivity) normally sits below this activity, so closing this one returns to it.
    private void OpenWeeklyView()
    {
        if (isTaskRoot())
            startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void BuildCalendar()
    {
        LinearLayout calendar = binding.calendar;
        calendar.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        Calendar month = Calendar.getInstance();
        int today = month.get(Calendar.DAY_OF_MONTH);
        month.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstColumn = ToWeekDay(month.get(Calendar.DAY_OF_WEEK)).ordinal();

        setTitle(GetMonthTitle(month));

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

        // When the view opens, center it on today. Not on later rebuilds, so coming back from a day
        // keeps the scroll position.
        if (!ScrolledToToday && todayCell != null)
        {
            ScrolledToToday = true;
            View cellToCenter = todayCell;
            calendar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    calendar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    CenterOn(cellToCenter);
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
        binding.contentMonth.scrollTo(cellRect.centerX() - binding.contentMonth.getWidth() / 2, 0);
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
            TextView choreView = (TextView) inflater.inflate(R.layout.calendar_day_chore, cell.dayChores, false);
            choreView.setText(chore.GetDescription());
            SetChoreIcon(choreView, chore.GetIcon());
            cell.dayChores.addView(choreView);
        }
        return cell.getRoot();
    }

    private void SetChoreIcon(TextView choreView, ChoreIconEnum iconType)
    {
        if (iconType == null)
            return;
        Drawable icon = ChoreIconDictionary.Instance().GetIcon(iconType);
        if (icon == null || icon.getConstantState() == null)
            return;
        // Each cell needs its own copy: the dictionary's drawables are shared and get resized here.
        Drawable copy = icon.getConstantState().newDrawable(getResources()).mutate();
        int size = getResources().getDimensionPixelSize(R.dimen.calendar_chore_icon_size);
        copy.setBounds(0, 0, size, size);
        choreView.setCompoundDrawablesRelative(copy, null, null, null);
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
