package com.zsoftware.choresrpg.schedule;

import android.content.Context;
import android.text.TextUtils;

import com.zsoftware.choresrpg.ChoreSchedule;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.WeekDay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Describes a chore's schedule in words, e.g. "Wednesday, Saturday", "Every 2 weeks on Monday (from
// 23 Nov 2026)", "Every month on day 1, 15" or "Once, on Wednesday, 25 November 2026".
public class ScheduleText {

    private static final int[] FullDayNames = {
            R.string.week_day_button_monday_full,
            R.string.week_day_button_tuesday_full,
            R.string.week_day_button_wednesday_full,
            R.string.week_day_button_thursday_full,
            R.string.week_day_button_friday_full,
            R.string.week_day_button_saturday_full,
            R.string.week_day_button_sunday_full
    };

    public static String Describe(Context context, ChoreSchedule schedule)
    {
        if (schedule.IsEmpty())
            return context.getString(R.string.chore_no_days);
        if (!schedule.IsRepeating())
            return context.getString(R.string.schedule_once,
                    DateFormat.getDateInstance(DateFormat.FULL).format(schedule.GetStartCalendar().getTime()));
        String separator = context.getString(R.string.schedule_list_separator);
        int interval = schedule.GetInterval();
        String text;
        if (schedule.GetRepeat() == ChoreSchedule.RepeatKind.Monthly)
        {
            List<String> days = new ArrayList<>();
            for (int day : schedule.GetMonthDays())
                days.add(String.valueOf(day));
            String dayList = TextUtils.join(separator, days);
            text = interval <= 1
                    ? context.getString(R.string.schedule_every_month, dayList)
                    : context.getResources().getQuantityString(R.plurals.schedule_every_n_months, interval, interval, dayList);
            if (interval > 1)
                text = context.getString(R.string.schedule_from, text,
                        new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(schedule.GetStartCalendar().getTime()));
            return text;
        }

        List<String> days = new ArrayList<>();
        for (WeekDay day : schedule.GetWeekDays())
            days.add(context.getString(FullDayNames[day.ordinal()]));
        String dayList = days.size() == WeekDay.values().length
                ? context.getString(R.string.chore_every_day)
                : TextUtils.join(separator, days);
        if (interval <= 1)
            return dayList;
        text = context.getResources().getQuantityString(R.plurals.schedule_every_n_weeks, interval, interval,
                days.size() == WeekDay.values().length ? dayList.toLowerCase(Locale.getDefault()) : dayList);
        return context.getString(R.string.schedule_from, text,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(schedule.GetStartCalendar().getTime()));
    }
}
