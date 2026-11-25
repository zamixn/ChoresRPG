package com.zsoftware.choresrpg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

// When a chore is due. Either:
// - Weekly: on some weekdays, every week or every n weeks (e.g. every 2 weeks on Monday);
// - Monthly: on some days of the month, every month or every n months (e.g. every 2 months on the 1st
//   and the 15th). Days past the end of a shorter month (29 - 31) fall on its last day;
// - Once: only on the start date (doesn't repeat).
// "Every n" counts from the start date: weeks from the week it's in (weeks start on Monday), months from
// its month. With n = 1 the start date doesn't matter.
public class ChoreSchedule {

    public enum RepeatKind
    {
        Weekly,
        Monthly,
        Once
    }

    public boolean IsRepeating() { return Repeat != RepeatKind.Once; }

    private RepeatKind Repeat = RepeatKind.Weekly;
    public RepeatKind GetRepeat() { return Repeat; }
    public void SetRepeat(RepeatKind repeat) { Repeat = repeat; }

    // Every how many weeks / months (1 = every week / month).
    private int Interval = 1;
    public int GetInterval() { return Interval; }
    public void SetInterval(int interval) { Interval = Math.max(1, interval); }

    private final List<WeekDay> WeekDays = new ArrayList<>();
    public List<WeekDay> GetWeekDays() { return WeekDays; }
    public void SetWeekDays(List<WeekDay> weekDays)
    {
        WeekDays.clear();
        for (WeekDay day : WeekDay.values())
        {
            if (weekDays.contains(day))
                WeekDays.add(day);
        }
    }

    // Days of the month (1 - 31), in order.
    private final List<Integer> MonthDays = new ArrayList<>();
    public List<Integer> GetMonthDays() { return Collections.unmodifiableList(MonthDays); }
    public void SetMonthDays(List<Integer> monthDays)
    {
        MonthDays.clear();
        for (Integer day : monthDays)
        {
            if (day != null && day >= 1 && day <= 31 && !MonthDays.contains(day))
                MonthDays.add(day);
        }
        Collections.sort(MonthDays);
    }

    // Start date as yyyymmdd (e.g. 20261125); 0 = not set (counts from 1970).
    private int StartDate = 0;
    public int GetStartDate() { return StartDate; }
    public void SetStartDate(int startDate) { StartDate = startDate; }
    public void SetStartDate(Calendar date) { StartDate = ToDateNumber(date); }

    public ChoreSchedule Copy()
    {
        ChoreSchedule copy = new ChoreSchedule();
        copy.Repeat = Repeat;
        copy.Interval = Interval;
        copy.WeekDays.addAll(WeekDays);
        copy.MonthDays.addAll(MonthDays);
        copy.StartDate = StartDate;
        return copy;
    }

    // True when the chore is due on that date.
    public boolean IsOnDate(Calendar date)
    {
        if (Repeat == RepeatKind.Once)
            return StartDate != 0 && ToDateNumber(date) == StartDate;
        if (Repeat == RepeatKind.Monthly)
        {
            if (Interval > 1 && FloorMod(MonthsSinceStart(date), Interval) != 0)
                return false;
            int day = date.get(Calendar.DAY_OF_MONTH);
            int lastDay = date.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int monthDay : MonthDays)
            {
                if (Math.min(monthDay, lastDay) == day)
                    return true;
            }
            return false;
        }
        if (!WeekDays.contains(WeekDay.FromCalendar(date)))
            return false;
        return Interval <= 1 || FloorMod(WeeksSinceStart(date), Interval) == 0;
    }

    // True when it's due on no day at all (nothing picked).
    public boolean IsEmpty()
    {
        if (Repeat == RepeatKind.Once)
            return StartDate == 0;
        return Repeat == RepeatKind.Monthly ? MonthDays.isEmpty() : WeekDays.isEmpty();
    }

    private long WeeksSinceStart(Calendar date)
    {
        long day = EpochDay(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
        long start = StartEpochDay();
        // Monday of each week (1970-01-01, epoch day 0, was a Thursday).
        long dayMonday = day - FloorMod(day + 3, 7);
        long startMonday = start - FloorMod(start + 3, 7);
        return Math.floorDiv(dayMonday - startMonday, 7);
    }

    private long MonthsSinceStart(Calendar date)
    {
        int startYear = StartDate == 0 ? 1970 : StartDate / 10000;
        int startMonth = StartDate == 0 ? 1 : StartDate / 100 % 100;
        return (date.get(Calendar.YEAR) - startYear) * 12L + (date.get(Calendar.MONTH) + 1 - startMonth);
    }

    private long StartEpochDay()
    {
        if (StartDate == 0)
            return 0;
        return EpochDay(StartDate / 10000, StartDate / 100 % 100, StartDate % 100);
    }

    // Days since 1970-01-01 for a calendar date (no time zones involved).
    static long EpochDay(int year, int month, int day)
    {
        long y = month <= 2 ? year - 1 : year;
        long era = Math.floorDiv(y, 400);
        long yearOfEra = y - era * 400;
        long dayOfYear = (153L * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
        return era * 146097 + dayOfEra - 719468;
    }

    private static long FloorMod(long value, long divisor)
    {
        return Math.floorMod(value, divisor);
    }

    public static int ToDateNumber(Calendar date)
    {
        return date.get(Calendar.YEAR) * 10000 + (date.get(Calendar.MONTH) + 1) * 100 + date.get(Calendar.DAY_OF_MONTH);
    }

    // The start date as a Calendar (today when not set).
    public Calendar GetStartCalendar()
    {
        Calendar date = AppClock.Now();
        if (StartDate != 0)
            date.set(StartDate / 10000, StartDate / 100 % 100 - 1, StartDate % 100);
        return date;
    }

    // Save format (the end of Chore's save line; the weekdays are saved by Chore):
    // Repeat/Interval/MonthDays/StartDate, MonthDays separated by ',' ("null" when empty), StartDate "null"
    // when not set.
    public String ToSaveString()
    {
        StringBuilder days = new StringBuilder();
        for (int day : MonthDays)
        {
            if (days.length() > 0)
                days.append(',');
            days.append(day);
        }
        return Repeat.name() + "/" + Interval + "/" + (days.length() == 0 ? "null" : days) + "/"
                + (StartDate == 0 ? "null" : String.valueOf(StartDate));
    }

    // Reads the fields saved by ToSaveString, starting at parts[index]; missing parts (older saves) keep
    // the defaults: weekly, every week.
    public void ParseFrom(String[] parts, int index)
    {
        if (parts.length > index && !parts[index].isEmpty() && !parts[index].equals("null"))
            Repeat = RepeatKind.valueOf(parts[index]);
        if (parts.length > index + 1 && !parts[index + 1].isEmpty() && !parts[index + 1].equals("null"))
            SetInterval(Integer.parseInt(parts[index + 1]));
        if (parts.length > index + 2 && !parts[index + 2].isEmpty() && !parts[index + 2].equals("null"))
        {
            List<Integer> days = new ArrayList<>();
            for (String day : parts[index + 2].split(","))
                days.add(Integer.parseInt(day));
            SetMonthDays(days);
        }
        if (parts.length > index + 3 && !parts[index + 3].isEmpty() && !parts[index + 3].equals("null"))
            StartDate = Integer.parseInt(parts[index + 3]);
    }
}
