package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

public class AppClockTest {

    @After
    public void ResetClock()
    {
        AppClock.SetDayOffset(0);
    }

    private static long DaysBetween(Calendar from, Calendar to)
    {
        return Math.round((to.getTimeInMillis() - from.getTimeInMillis()) / 86400000.0);
    }

    @Test
    public void Now_IsMovedByTheDayOffset()
    {
        Calendar real = Calendar.getInstance();
        AppClock.SetDayOffset(3);
        assertEquals(3, DaysBetween(real, AppClock.Now()));
        AppClock.SetDayOffset(-2);
        assertEquals(-2, DaysBetween(real, AppClock.Now()));
    }

    @Test
    public void Complete_UsesTheSimulatedDay()
    {
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash);
        AppClock.SetDayOffset(1);
        chore.Complete();
        assertTrue(chore.IsCompletedToday());

        // Back on the real day the completion is tomorrow, not today.
        AppClock.SetDayOffset(0);
        assertFalse(chore.IsCompletedToday());
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        assertTrue(chore.IsCompletedOn(tomorrow));
    }

    @Test
    public void WeekDates_FollowTheSimulatedDay()
    {
        AppClock.SetDayOffset(7);
        Calendar real = Calendar.getInstance();
        Calendar monday = WeekDay.Monday.GetDateThisWeek();
        assertEquals(WeekDay.Monday, WeekDay.FromCalendar(monday));
        long days = DaysBetween(real, monday);
        assertTrue("Monday of next week, got " + days, days >= 1 && days <= 7);
    }
}
