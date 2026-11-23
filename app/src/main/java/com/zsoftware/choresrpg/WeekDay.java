package com.zsoftware.choresrpg;

import java.util.Calendar;

public enum WeekDay {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday;

    // Calendar.DAY_OF_WEEK runs Sunday (1) .. Saturday (7); WeekDay runs Monday .. Sunday.
    public static WeekDay FromCalendar(Calendar date)
    {
        return values()[(date.get(Calendar.DAY_OF_WEEK) + 5) % 7];
    }

    // The date of this weekday in the current week (weeks start on Monday, like the week view).
    public Calendar GetDateThisWeek()
    {
        Calendar date = Calendar.getInstance();
        int todayIndex = FromCalendar(date).ordinal();
        date.add(Calendar.DAY_OF_MONTH, ordinal() - todayIndex);
        return date;
    }
}
