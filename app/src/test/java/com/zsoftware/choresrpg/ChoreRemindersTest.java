package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.reminders.ChoreReminders;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ChoreRemindersTest {

    private static Calendar At(int year, int month, int day, int hour, int minute)
    {
        Calendar time = Calendar.getInstance();
        time.set(year, month, day, hour, minute, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time;
    }

    @Test
    public void ChoreLines_OnlyThatDay_UnfinishedFirst()
    {
        Calendar monday = At(2026, Calendar.NOVEMBER, 23, 9, 0);
        Chore dishes = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash);
        Chore trash = new Chore("Trash", Arrays.asList(WeekDay.Monday), ChoreIconEnum.Delete);
        Chore laundry = new Chore("Laundry", Arrays.asList(WeekDay.Sunday), ChoreIconEnum.LocalLaundryService);
        dishes.SetLastCompleted(monday.getTimeInMillis());

        List<String> lines = ChoreReminders.BuildChoreLines(
                Arrays.asList(dishes, trash, laundry), monday, "[ ] %1$s", "[x] %1$s");
        assertEquals(Arrays.asList("[ ] Trash", "[x] Dishes"), lines);
    }

    @Test
    public void NextTime_LaterToday()
    {
        Calendar now = At(2026, Calendar.NOVEMBER, 23, 9, 30);
        long next = ChoreReminders.GetNextTime(10 * 60, now.getTimeInMillis());
        assertEquals(At(2026, Calendar.NOVEMBER, 23, 10, 0).getTimeInMillis(), next);
    }

    @Test
    public void NextTime_AlreadyPassed_Tomorrow()
    {
        Calendar now = At(2026, Calendar.NOVEMBER, 23, 20, 0);
        long next = ChoreReminders.GetNextTime(20 * 60, now.getTimeInMillis());
        assertEquals(At(2026, Calendar.NOVEMBER, 24, 20, 0).getTimeInMillis(), next);
    }
}
