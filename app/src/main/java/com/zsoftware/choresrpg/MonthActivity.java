package com.zsoftware.choresrpg;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
            Toast.makeText(this, R.string.all_data_deleted, Toast.LENGTH_SHORT).show();
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

        LinearLayout header = NewWeekRow();
        for (int column = 0; column < DaysInWeek; column++)
        {
            TextView dayName = (TextView) inflater.inflate(R.layout.calendar_week_day_header, header, false);
            dayName.setText(WeekDayHeaderStrings[column]);
            header.addView(dayName, NewCellLayoutParams());
        }
        calendar.addView(header);

        List<Chore> chores = SaveData.Instance().GetChoreList();
        int rowCount = (firstColumn + daysInMonth + DaysInWeek - 1) / DaysInWeek;
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
                    cell = CreateDayCell(inflater, week, dayOfMonth, WeekDay.values()[column], chores, dayOfMonth == today);
                week.addView(cell, NewCellLayoutParams());
            }
            calendar.addView(week);
        }
    }

    private View CreateDayCell(LayoutInflater inflater, ViewGroup parent, int dayOfMonth, WeekDay weekDay, List<Chore> chores, boolean isToday)
    {
        CalendarDayCellBinding cell = CalendarDayCellBinding.inflate(inflater, parent, false);
        cell.dayNumber.setText(String.valueOf(dayOfMonth));
        if (isToday)
            cell.getRoot().setBackgroundResource(R.drawable.calendar_day_today);

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
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // Seven equal-width columns; cells stretch to the tallest cell in the week so the borders line up.
    private LinearLayout.LayoutParams NewCellLayoutParams()
    {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
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
