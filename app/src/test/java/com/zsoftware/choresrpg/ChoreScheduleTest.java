package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

public class ChoreScheduleTest {

    private static Calendar Date(int year, int month, int day)
    {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month - 1, day, 12, 0);
        return date;
    }

    private static ChoreSchedule Weekly(int interval, int startDate, WeekDay... days)
    {
        ChoreSchedule schedule = new ChoreSchedule();
        schedule.SetWeekDays(Arrays.asList(days));
        schedule.SetInterval(interval);
        schedule.SetStartDate(startDate);
        return schedule;
    }

    private static ChoreSchedule Monthly(int interval, int startDate, Integer... days)
    {
        ChoreSchedule schedule = new ChoreSchedule();
        schedule.SetRepeat(ChoreSchedule.RepeatKind.Monthly);
        schedule.SetMonthDays(Arrays.asList(days));
        schedule.SetInterval(interval);
        schedule.SetStartDate(startDate);
        return schedule;
    }

    @Test
    public void EpochDay_KnownDates()
    {
        assertEquals(0, ChoreSchedule.EpochDay(1970, 1, 1));
        assertEquals(11017, ChoreSchedule.EpochDay(2000, 3, 1));
        assertEquals(20417, ChoreSchedule.EpochDay(2025, 11, 25));
        assertEquals(-1, ChoreSchedule.EpochDay(1969, 12, 31));
    }

    @Test
    public void Weekly_EveryWeek_OnItsWeekdays()
    {
        ChoreSchedule schedule = Weekly(1, 0, WeekDay.Monday, WeekDay.Thursday);
        assertTrue(schedule.IsOnDate(Date(2026, 11, 23)));    // Monday
        assertTrue(schedule.IsOnDate(Date(2026, 11, 26)));    // Thursday
        assertFalse(schedule.IsOnDate(Date(2026, 11, 25)));   // Wednesday
        assertTrue(schedule.IsOnDate(Date(2027, 3, 1)));      // a Monday months later
    }

    @Test
    public void Weekly_EveryTwoWeeks_CountsFromTheStartWeek()
    {
        // Started on Wednesday 25 Nov 2026: that week (from Monday 23 Nov) is an "on" week.
        ChoreSchedule schedule = Weekly(2, 20261125, WeekDay.Monday);
        assertTrue(schedule.IsOnDate(Date(2026, 11, 23)));
        assertFalse(schedule.IsOnDate(Date(2026, 11, 30)));
        assertTrue(schedule.IsOnDate(Date(2026, 12, 7)));
        assertFalse(schedule.IsOnDate(Date(2026, 11, 16)));
        assertTrue(schedule.IsOnDate(Date(2026, 11, 9)));     // before the start too
        assertTrue(schedule.IsOnDate(Date(2027, 1, 4)));      // across the new year
    }

    @Test
    public void Weekly_EveryThreeWeeks()
    {
        ChoreSchedule schedule = Weekly(3, 20261123, WeekDay.Sunday);
        assertTrue(schedule.IsOnDate(Date(2026, 11, 29)));    // Sunday of the start week
        assertFalse(schedule.IsOnDate(Date(2026, 12, 6)));
        assertFalse(schedule.IsOnDate(Date(2026, 12, 13)));
        assertTrue(schedule.IsOnDate(Date(2026, 12, 20)));
    }

    @Test
    public void Monthly_OnItsDays()
    {
        ChoreSchedule schedule = Monthly(1, 0, 1, 15);
        assertTrue(schedule.IsOnDate(Date(2026, 11, 1)));
        assertTrue(schedule.IsOnDate(Date(2026, 11, 15)));
        assertFalse(schedule.IsOnDate(Date(2026, 11, 14)));
        assertTrue(schedule.IsOnDate(Date(2027, 2, 15)));
    }

    @Test
    public void Monthly_DaysPastTheEnd_FallOnTheLastDay()
    {
        ChoreSchedule schedule = Monthly(1, 0, 31);
        assertTrue(schedule.IsOnDate(Date(2026, 12, 31)));
        assertFalse(schedule.IsOnDate(Date(2026, 12, 30)));
        assertTrue(schedule.IsOnDate(Date(2026, 11, 30)));
        assertTrue(schedule.IsOnDate(Date(2027, 2, 28)));
        assertTrue(schedule.IsOnDate(Date(2028, 2, 29)));     // leap year
        assertFalse(schedule.IsOnDate(Date(2028, 2, 28)));
    }

    @Test
    public void Monthly_EveryTwoMonths_CountsFromTheStartMonth()
    {
        ChoreSchedule schedule = Monthly(2, 20261110, 5);
        assertTrue(schedule.IsOnDate(Date(2026, 11, 5)));     // the start month (day before the start date counts)
        assertFalse(schedule.IsOnDate(Date(2026, 12, 5)));
        assertTrue(schedule.IsOnDate(Date(2027, 1, 5)));
        assertTrue(schedule.IsOnDate(Date(2026, 9, 5)));
        assertFalse(schedule.IsOnDate(Date(2026, 10, 5)));
    }

    @Test
    public void Once_OnlyOnItsDate()
    {
        ChoreSchedule schedule = Weekly(1, 20261125, WeekDay.Wednesday);
        schedule.SetRepeat(ChoreSchedule.RepeatKind.Once);
        assertFalse(schedule.IsRepeating());
        assertTrue(schedule.IsOnDate(Date(2026, 11, 25)));
        assertFalse(schedule.IsOnDate(Date(2026, 12, 2)));    // a week later: same weekday, but no
        assertFalse(schedule.IsOnDate(Date(2025, 11, 25)));   // a year earlier
        assertFalse(schedule.IsEmpty());
    }

    @Test
    public void Chore_RoundTrip_KeepsOnce()
    {
        ChoreSchedule once = new ChoreSchedule();
        once.SetRepeat(ChoreSchedule.RepeatKind.Once);
        once.SetStartDate(20261224);
        Chore chore = new Chore("Buy the tree", once, ChoreIconEnum.Celebration);
        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        assertEquals(ChoreSchedule.RepeatKind.Once, loaded.GetSchedule().GetRepeat());
        assertTrue(loaded.IsOnDate(Date(2026, 12, 24)));
        assertFalse(loaded.IsOnDate(Date(2026, 12, 25)));
        assertFalse(loaded.IsOnDay(WeekDay.Thursday));
    }

    @Test
    public void MonthDays_SortedUniqueAndInRange()
    {
        ChoreSchedule schedule = Monthly(1, 0, 15, 1, 15, 0, 32);
        assertEquals(Arrays.asList(1, 15), schedule.GetMonthDays());
    }

    @Test
    public void Chore_RoundTrip_KeepsMonthlySchedule()
    {
        Chore chore = new Chore("Pay rent", Monthly(2, 20261101, 1, 28), ChoreIconEnum.Payments);
        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        ChoreSchedule schedule = loaded.GetSchedule();
        assertEquals(ChoreSchedule.RepeatKind.Monthly, schedule.GetRepeat());
        assertEquals(2, schedule.GetInterval());
        assertEquals(Arrays.asList(1, 28), schedule.GetMonthDays());
        assertEquals(20261101, schedule.GetStartDate());
        assertTrue(loaded.HasSameContent(chore));
        assertTrue(loaded.IsOnDate(Date(2027, 1, 28)));
        assertFalse(loaded.IsOnDay(WeekDay.Monday));   // monthly chores aren't "on" weekdays
    }

    @Test
    public void Chore_RoundTrip_KeepsEveryNWeeks()
    {
        Chore chore = new Chore("Change sheets", Weekly(2, 20261125, WeekDay.Saturday), ChoreIconEnum.Bed);
        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        assertEquals(2, loaded.GetSchedule().GetInterval());
        assertEquals(Collections.singletonList(WeekDay.Saturday), loaded.GetWeekDays());
        assertTrue(loaded.IsOnDate(Date(2026, 11, 28)));
        assertFalse(loaded.IsOnDate(Date(2026, 12, 5)));
    }

    @Test
    public void Chore_SavedBeforeSchedules_IsWeeklyEveryWeek()
    {
        Chore loaded = Chore.Parse("Mop/CleaningServices/Monday/null/null/null/null/null/null/abc-id/null/null/null/99/group1");
        ChoreSchedule schedule = loaded.GetSchedule();
        assertEquals(ChoreSchedule.RepeatKind.Weekly, schedule.GetRepeat());
        assertEquals(1, schedule.GetInterval());
        assertTrue(loaded.IsOnDate(Date(2026, 11, 23)));
        assertEquals("group1", loaded.GetGroupId());
    }

    @Test
    public void Chore_ScheduleChange_IsAContentChange()
    {
        Chore chore = new Chore("Mop", Weekly(1, 0, WeekDay.Monday), ChoreIconEnum.CleaningServices);
        Chore other = Chore.Parse(chore.ToSaveDataString());
        ChoreSchedule schedule = other.GetSchedule();
        schedule.SetInterval(2);
        other.SetSchedule(schedule);
        assertFalse(chore.HasSameContent(other));
    }

    @Test
    public void Merge_TakesTheNewerSchedule()
    {
        Chore mine = new Chore("Mop", Weekly(1, 0, WeekDay.Monday), ChoreIconEnum.CleaningServices);
        mine.SetUpdatedAt(100);
        Chore theirs = Chore.Parse(mine.ToSaveDataString());
        theirs.SetSchedule(Monthly(1, 0, 10));
        theirs.SetUpdatedAt(200);
        assertEquals(ChoreSchedule.RepeatKind.Monthly, Chore.Merge(mine, theirs).GetSchedule().GetRepeat());
        assertEquals(ChoreSchedule.RepeatKind.Monthly, Chore.Merge(theirs, mine).GetSchedule().GetRepeat());
    }

    @Test
    public void CopyToGroup_KeepsSchedule()
    {
        Chore chore = new Chore("Pay rent", Monthly(3, 20260101, 1), ChoreIconEnum.Payments);
        Chore copy = chore.CopyToGroup("group2");
        assertEquals(3, copy.GetSchedule().GetInterval());
        assertEquals(Collections.singletonList(1), copy.GetSchedule().GetMonthDays());
    }
}
