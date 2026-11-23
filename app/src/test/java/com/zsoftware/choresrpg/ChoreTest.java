package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

public class ChoreTest {

    @Test
    public void NewChores_GetUniqueIds()
    {
        Chore a = new Chore("A", Arrays.asList(WeekDay.Monday), ChoreIconEnum.CleaningServices);
        Chore b = new Chore("B", Arrays.asList(WeekDay.Monday), ChoreIconEnum.CleaningServices);
        assertNotNull(a.GetId());
        assertNotEquals(a.GetId(), b.GetId());
    }

    @Test
    public void RoundTrip_KeepsIdAndLastCompleted()
    {
        Chore chore = new Chore("Mop the floor", Arrays.asList(WeekDay.Wednesday, WeekDay.Saturday), ChoreIconEnum.Kitchen);
        chore.SetLastCompleted(1795000000000L);

        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        assertEquals(chore.GetId(), loaded.GetId());
        assertEquals(Long.valueOf(1795000000000L), loaded.GetLastCompleted());
        assertEquals(chore.GetWeekDays(), loaded.GetWeekDays());
        assertFalse(loaded.WasLoadedWithoutId());
    }

    @Test
    public void RoundTrip_NeverCompleted()
    {
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.CleaningServices);
        assertNull(Chore.Parse(chore.ToSaveDataString()).GetLastCompleted());
    }

    @Test
    public void Parse_OldFormatWithoutId_GetsNewId()
    {
        // Saved before chores had ids or completion dates.
        Chore loaded = Chore.Parse("Mop/FloorMop/null/Tuesday/null/null/null/Saturday/null");
        assertEquals("Mop", loaded.GetDescription());
        assertEquals(ChoreIconEnum.CleaningServices, loaded.GetIcon());
        assertEquals(Arrays.asList(WeekDay.Tuesday, WeekDay.Saturday), loaded.GetWeekDays());
        assertNotNull(loaded.GetId());
        assertNull(loaded.GetLastCompleted());
        assertTrue(loaded.WasLoadedWithoutId());
    }

    @Test
    public void History_KeepsEveryCompletionThroughRoundTrip()
    {
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash);
        chore.Complete();
        chore.SetLastCompleted(1795000000000L);   // moves the newest completion
        chore.Complete();
        chore.Complete();
        assertEquals(3, chore.GetCompletionHistory().size());

        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        assertEquals(chore.GetCompletionHistory(), loaded.GetCompletionHistory());
        assertEquals(chore.GetLastCompleted(), loaded.GetLastCompleted());
        assertEquals(Long.valueOf(1795000000000L), loaded.GetCompletionHistory().get(0));
    }

    @Test
    public void History_RemoveLastCompletion_FallsBackToPreviousOne()
    {
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash);
        chore.SetLastCompleted(1000L);
        chore.Complete();
        chore.RemoveLastCompletion();
        assertEquals(Long.valueOf(1000L), chore.GetLastCompleted());
        chore.RemoveLastCompletion();
        assertNull(chore.GetLastCompleted());
        chore.RemoveLastCompletion();   // nothing left: no error
        assertTrue(chore.GetCompletionHistory().isEmpty());
    }

    @Test
    public void History_OldFormatWithOnlyLastCompleted()
    {
        // Saved before the history existed: the last completion becomes the only history entry.
        Chore loaded = Chore.Parse("Mop/Kitchen/Monday/null/null/null/null/null/null/abc-id/1795000000000");
        assertEquals("abc-id", loaded.GetId());
        assertEquals(Arrays.asList(1795000000000L), loaded.GetCompletionHistory());
    }

    @Test
    public void IsCompletedOn_AnyCompletionInHistory()
    {
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash);
        Calendar monday = Calendar.getInstance();
        monday.set(2026, Calendar.NOVEMBER, 23, 9, 0);
        Calendar tuesday = (Calendar) monday.clone();
        tuesday.add(Calendar.DAY_OF_MONTH, 1);
        chore.SetLastCompleted(monday.getTimeInMillis());
        chore.Complete();
        chore.SetLastCompleted(tuesday.getTimeInMillis());
        assertTrue(chore.IsCompletedOn(monday));
        assertTrue(chore.IsCompletedOn(tuesday));
    }

    @Test
    public void Complete_SetsLastCompletedToNow()
    {
        Chore chore = new Chore("Trash", Arrays.asList(WeekDay.Monday), ChoreIconEnum.CleaningServices);
        long before = System.currentTimeMillis();
        chore.Complete();
        assertTrue(chore.GetLastCompleted() >= before);
    }

    @Test
    public void IsCompletedOn_OnlyTheSameCalendarDay()
    {
        Chore chore = new Chore("Trash", Arrays.asList(WeekDay.Monday), ChoreIconEnum.CleaningServices);
        Calendar day = Calendar.getInstance();
        day.set(2026, Calendar.NOVEMBER, 23, 21, 30);
        assertFalse(chore.IsCompletedOn(day));

        Calendar completed = (Calendar) day.clone();
        completed.set(Calendar.HOUR_OF_DAY, 8);
        chore.SetLastCompleted(completed.getTimeInMillis());
        assertTrue(chore.IsCompletedOn(day));

        Calendar nextDay = (Calendar) day.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        assertFalse(chore.IsCompletedOn(nextDay));

        Calendar nextYear = (Calendar) day.clone();
        nextYear.add(Calendar.YEAR, 1);
        assertFalse(chore.IsCompletedOn(nextYear));
    }

    @Test
    public void IsCompletedToday_AfterComplete()
    {
        Chore chore = new Chore("Trash", Arrays.asList(WeekDay.Monday), ChoreIconEnum.CleaningServices);
        assertFalse(chore.IsCompletedToday());
        chore.Complete();
        assertTrue(chore.IsCompletedToday());
    }

    @Test
    public void WeekDay_FromCalendar_StartsOnMonday()
    {
        Calendar date = Calendar.getInstance();
        date.set(2026, Calendar.NOVEMBER, 23); // a Monday
        assertEquals(WeekDay.Monday, WeekDay.FromCalendar(date));
        date.set(2026, Calendar.NOVEMBER, 29); // a Sunday
        assertEquals(WeekDay.Sunday, WeekDay.FromCalendar(date));
    }

    @Test
    public void WeekDay_GetDateThisWeek_IsInTheCurrentWeek()
    {
        Calendar today = Calendar.getInstance();
        int todayIndex = WeekDay.FromCalendar(today).ordinal();
        for (WeekDay day : WeekDay.values())
        {
            Calendar date = day.GetDateThisWeek();
            assertEquals(day, WeekDay.FromCalendar(date));
            long daysFromToday = Math.round((date.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
            assertEquals(day.ordinal() - todayIndex, daysFromToday);
        }
    }

    @Test
    public void Icons_RemovedIconNamesLoadAsReplacements()
    {
        // Chores saved with the app's first four icons, which were removed.
        assertEquals(ChoreIconEnum.CleaningServices, ChoreIconEnum.FromSavedName("Cleaning"));
        assertEquals(ChoreIconEnum.Spa, ChoreIconEnum.FromSavedName("Sigh"));
        assertEquals(ChoreIconEnum.CleaningServices, ChoreIconEnum.FromSavedName("FloorMop"));
        assertEquals(ChoreIconEnum.Kitchen, ChoreIconEnum.FromSavedName("Fridge"));
        assertEquals(ChoreIconEnum.Kitchen, Chore.Parse("Defrost/Fridge/Monday/null/null/null/null/null/null").GetIcon());
        assertEquals(ChoreIconEnum.Pets, ChoreIconEnum.FromSavedName("Pets"));
        assertTrue(ChoreIconEnum.values().length > 100);
    }

    @Test
    public void Icons_EveryIconSurvivesRoundTrip()
    {
        for (ChoreIconEnum icon : ChoreIconEnum.values())
        {
            Chore chore = new Chore("Chore", Arrays.asList(WeekDay.Monday), icon);
            assertEquals(icon, Chore.Parse(chore.ToSaveDataString()).GetIcon());
        }
    }

    @Test
    public void Icons_EveryIconHasTags()
    {
        for (ChoreIconEnum icon : ChoreIconEnum.values())
            assertTrue(icon.name(), icon.GetTags().length >= 2);
    }

    @Test
    public void IconSearch_MatchesTagPrefixesOfEveryWord()
    {
        assertTrue(ChoreIconEnum.LocalLaundryService.MatchesSearch("laundry"));
        assertTrue(ChoreIconEnum.LocalLaundryService.MatchesSearch("Wash"));
        assertTrue(ChoreIconEnum.LocalLaundryService.MatchesSearch("  wash   cloth "));
        assertTrue(ChoreIconEnum.Pets.MatchesSearch("dog"));
        assertTrue(ChoreIconEnum.Delete.MatchesSearch("trash"));
        assertFalse(ChoreIconEnum.Pets.MatchesSearch("laundry"));
        assertFalse(ChoreIconEnum.LocalLaundryService.MatchesSearch("wash dog"));
    }

    @Test
    public void IconSearch_EmptyTextMatchesEverything()
    {
        for (ChoreIconEnum icon : ChoreIconEnum.values())
        {
            assertTrue(icon.MatchesSearch(""));
            assertTrue(icon.MatchesSearch("   "));
        }
    }

    @Test
    public void Description_SeparatorsAreReplaced()
    {
        Chore chore = new Chore("Wash/dry; fold", Arrays.asList(WeekDay.Sunday), ChoreIconEnum.Spa);
        assertEquals("Wash-dry, fold", chore.GetDescription());
        chore.SetDescription(" a/b ");
        assertEquals("a-b", chore.GetDescription());
        assertEquals("a-b", Chore.Parse(chore.ToSaveDataString()).GetDescription());
    }
}
