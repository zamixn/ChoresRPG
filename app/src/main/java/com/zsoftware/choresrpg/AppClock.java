package com.zsoftware.choresrpg;

import java.util.Calendar;

// The app's "now". Every place that needs today's date or the current time asks this class, so the
// debug options "Simulate next day" / "Simulate previous day" can move the whole app to another day.
// The day offset is saved by SaveData and is always 0 in release builds.
public class AppClock {
    private static int DayOffset = 0;

    // How many days the app is moved from the real date (0 = the real date).
    public static int GetDayOffset() { return DayOffset; }
    public static void SetDayOffset(int dayOffset) { DayOffset = dayOffset; }

    // The current date and time, moved by the simulated day offset.
    public static Calendar Now()
    {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, DayOffset);
        return now;
    }

    public static long NowMillis()
    {
        return Now().getTimeInMillis();
    }
}
