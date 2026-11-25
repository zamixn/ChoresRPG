package com.zsoftware.choresrpg.schedule;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import com.zsoftware.choresrpg.AppClock;
import com.zsoftware.choresrpg.ChoreSchedule;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.WeekDay;
import com.zsoftware.choresrpg.databinding.MonthDayCircleBinding;
import com.zsoftware.choresrpg.databinding.ScheduleEditorBinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Edits when a chore repeats (a ChoreSchedule): Weekly | Monthly, the weekdays (with quick picks for every
// day, weekdays and the weekend) or the days of the month, every n weeks / months, and - for every 2+ - the
// start date it counts from. Used by the add-chore form and the chore view's edit popup; it adds its views
// to the given container.
public class ScheduleEditor {

    private static final int MaxInterval = 52;
    private static final String StateSchedule = "Schedule";
    private static final String StateWeekDays = "ScheduleWeekDays";

    private final Activity Activity;
    private final ScheduleEditorBinding binding;
    private final Map<WeekDay, TextView> WeekDayCircles = new EnumMap<>(WeekDay.class);
    private final List<TextView> MonthDayCircles = new ArrayList<>();
    private ChoreSchedule Schedule;

    public ScheduleEditor(Activity activity, LayoutInflater inflater, ViewGroup container, ChoreSchedule schedule)
    {
        Activity = activity;
        binding = ScheduleEditorBinding.inflate(inflater, container, true);
        Schedule = schedule.Copy();
        if (Schedule.GetStartDate() == 0)
            Schedule.SetStartDate(AppClock.Now());
        if (Schedule.IsRepeating())
            LastRepeatKind = Schedule.GetRepeat();

        binding.repeatToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || !Schedule.IsRepeating())
                return;
            LastRepeatKind = checkedId == R.id.repeat_monthly ? ChoreSchedule.RepeatKind.Monthly : ChoreSchedule.RepeatKind.Weekly;
            Schedule.SetRepeat(LastRepeatKind);
            Show();
        });
        binding.onceDateButton.setOnClickListener(view -> PickStartDate());

        WeekDayCircles.put(WeekDay.Monday, binding.buttonMonday);
        WeekDayCircles.put(WeekDay.Tuesday, binding.buttonTuesday);
        WeekDayCircles.put(WeekDay.Wednesday, binding.buttonWednesday);
        WeekDayCircles.put(WeekDay.Thursday, binding.buttonThursday);
        WeekDayCircles.put(WeekDay.Friday, binding.buttonFriday);
        WeekDayCircles.put(WeekDay.Saturday, binding.buttonSaturday);
        WeekDayCircles.put(WeekDay.Sunday, binding.buttonSunday);
        for (Map.Entry<WeekDay, TextView> entry : WeekDayCircles.entrySet())
        {
            WeekDay day = entry.getKey();
            entry.getValue().setOnClickListener(view -> ToggleWeekDay(day));
        }
        binding.everyDayButton.setOnClickListener(view -> SetWeekDays(Arrays.asList(WeekDay.values())));
        binding.weekdaysButton.setOnClickListener(view -> SetWeekDays(Arrays.asList(
                WeekDay.Monday, WeekDay.Tuesday, WeekDay.Wednesday, WeekDay.Thursday, WeekDay.Friday)));
        binding.weekendButton.setOnClickListener(view -> SetWeekDays(Arrays.asList(WeekDay.Saturday, WeekDay.Sunday)));

        // 1 - 31, seven per row.
        for (int day = 1; day <= 31; day++)
        {
            MonthDayCircleBinding cell = MonthDayCircleBinding.inflate(inflater, binding.monthDaysGrid, false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            cell.getRoot().setLayoutParams(params);
            cell.dayCircle.setText(String.valueOf(day));
            int monthDay = day;
            cell.dayCircle.setOnClickListener(view -> ToggleMonthDay(monthDay));
            binding.monthDaysGrid.addView(cell.getRoot());
            MonthDayCircles.add(cell.dayCircle);
        }

        binding.intervalMinus.setOnClickListener(view -> SetInterval(Schedule.GetInterval() - 1));
        binding.intervalPlus.setOnClickListener(view -> SetInterval(Schedule.GetInterval() + 1));
        binding.startButton.setOnClickListener(view -> PickStartDate());

        ShowRepeatChoice();
        // Off: due once, on the start date; on again: back to weekly / monthly as it was.
        binding.repeatsSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            Schedule.SetRepeat(isChecked ? LastRepeatKind : ChoreSchedule.RepeatKind.Once);
            ShowRepeatChoice();
            Show();
        });
        Show();
    }

    // Weekly or monthly, for when "Repeats" is (turned) on.
    private ChoreSchedule.RepeatKind LastRepeatKind = ChoreSchedule.RepeatKind.Weekly;

    private void ShowRepeatChoice()
    {
        binding.repeatsSwitch.setChecked(Schedule.IsRepeating());
        binding.repeatToggle.check(LastRepeatKind == ChoreSchedule.RepeatKind.Monthly ? R.id.repeat_monthly : R.id.repeat_weekly);
    }

    // The schedule as edited so far.
    public ChoreSchedule GetSchedule()
    {
        return Schedule.Copy();
    }

    // Keeps the edits over a rotation (call from onSaveInstanceState / after creating the editor again).
    public void SaveState(Bundle state)
    {
        state.putString(StateSchedule, Schedule.ToSaveString());
        ArrayList<String> days = new ArrayList<>();
        for (WeekDay day : Schedule.GetWeekDays())
            days.add(day.name());
        state.putStringArrayList(StateWeekDays, days);
    }

    public void RestoreState(Bundle state)
    {
        if (state == null || state.getString(StateSchedule) == null)
            return;
        ChoreSchedule restored = new ChoreSchedule();
        restored.ParseFrom(state.getString(StateSchedule).split("/"), 0);
        List<WeekDay> days = new ArrayList<>();
        if (state.getStringArrayList(StateWeekDays) != null)
        {
            for (String day : state.getStringArrayList(StateWeekDays))
                days.add(WeekDay.valueOf(day));
        }
        restored.SetWeekDays(days);
        Schedule = restored;
        if (Schedule.IsRepeating())
            LastRepeatKind = Schedule.GetRepeat();
        ShowRepeatChoice();
        Show();
    }

    private void ToggleWeekDay(WeekDay day)
    {
        List<WeekDay> days = new ArrayList<>(Schedule.GetWeekDays());
        if (days.contains(day))
            days.remove(day);
        else
            days.add(day);
        SetWeekDays(days);
    }

    private void SetWeekDays(List<WeekDay> days)
    {
        Schedule.SetWeekDays(days);
        Show();
    }

    private void ToggleMonthDay(int day)
    {
        List<Integer> days = new ArrayList<>(Schedule.GetMonthDays());
        if (days.contains(day))
            days.remove(Integer.valueOf(day));
        else
            days.add(day);
        Schedule.SetMonthDays(days);
        Show();
    }

    private void SetInterval(int interval)
    {
        Schedule.SetInterval(Math.min(MaxInterval, Math.max(1, interval)));
        Show();
    }

    // Weekly: the date picked (any day of the week counts as that week). Monthly: its month.
    private void PickStartDate()
    {
        Calendar start = Schedule.GetStartCalendar();
        new DatePickerDialog(Activity, (picker, year, month, dayOfMonth) -> {
            Calendar picked = (Calendar) start.clone();
            picked.set(year, month, dayOfMonth);
            Schedule.SetStartDate(picked);
            Show();
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void Show()
    {
        boolean repeating = Schedule.IsRepeating();
        boolean monthly = Schedule.GetRepeat() == ChoreSchedule.RepeatKind.Monthly;
        binding.onceRow.setVisibility(repeating ? View.GONE : View.VISIBLE);
        binding.onceDateButton.setText(DateFormat.getDateInstance(DateFormat.FULL).format(Schedule.GetStartCalendar().getTime()));
        binding.repeatToggle.setVisibility(repeating ? View.VISIBLE : View.GONE);
        binding.intervalRow.setVisibility(repeating ? View.VISIBLE : View.GONE);
        binding.weeklySection.setVisibility(repeating && !monthly ? View.VISIBLE : View.GONE);
        binding.monthlySection.setVisibility(repeating && monthly ? View.VISIBLE : View.GONE);

        for (Map.Entry<WeekDay, TextView> entry : WeekDayCircles.entrySet())
            entry.getValue().setSelected(Schedule.GetWeekDays().contains(entry.getKey()));
        for (int i = 0; i < MonthDayCircles.size(); i++)
            MonthDayCircles.get(i).setSelected(Schedule.GetMonthDays().contains(i + 1));

        int interval = Schedule.GetInterval();
        binding.intervalValue.setText(String.valueOf(interval));
        binding.intervalUnit.setText(Activity.getResources().getQuantityString(
                monthly ? R.plurals.schedule_unit_months : R.plurals.schedule_unit_weeks, interval));
        binding.intervalMinus.setEnabled(interval > 1);
        binding.intervalPlus.setEnabled(interval < MaxInterval);

        binding.startRow.setVisibility(repeating && interval > 1 ? View.VISIBLE : View.GONE);
        Calendar start = Schedule.GetStartCalendar();
        binding.startButton.setText(monthly
                ? new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(start.getTime())
                : DateFormat.getDateInstance(DateFormat.MEDIUM).format(start.getTime()));
    }
}
