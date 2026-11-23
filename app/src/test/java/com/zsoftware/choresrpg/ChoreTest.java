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

public class ChoreTest {

    @Test
    public void NewChores_GetUniqueIds()
    {
        Chore a = new Chore("A", Arrays.asList(WeekDay.Monday), ChoreIconEnum.Cleaning);
        Chore b = new Chore("B", Arrays.asList(WeekDay.Monday), ChoreIconEnum.Cleaning);
        assertNotNull(a.GetId());
        assertNotEquals(a.GetId(), b.GetId());
    }

    @Test
    public void RoundTrip_KeepsIdAndLastCompleted()
    {
        Chore chore = new Chore("Mop the floor", Arrays.asList(WeekDay.Wednesday, WeekDay.Saturday), ChoreIconEnum.FloorMop);
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
        Chore chore = new Chore("Dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Cleaning);
        assertNull(Chore.Parse(chore.ToSaveDataString()).GetLastCompleted());
    }

    @Test
    public void Parse_OldFormatWithoutId_GetsNewId()
    {
        // Saved before chores had ids or completion dates.
        Chore loaded = Chore.Parse("Mop/FloorMop/null/Tuesday/null/null/null/Saturday/null");
        assertEquals("Mop", loaded.GetDescription());
        assertEquals(Arrays.asList(WeekDay.Tuesday, WeekDay.Saturday), loaded.GetWeekDays());
        assertNotNull(loaded.GetId());
        assertNull(loaded.GetLastCompleted());
        assertTrue(loaded.WasLoadedWithoutId());
    }

    @Test
    public void Complete_SetsLastCompletedToNow()
    {
        Chore chore = new Chore("Trash", Arrays.asList(WeekDay.Monday), ChoreIconEnum.Cleaning);
        long before = System.currentTimeMillis();
        chore.Complete();
        assertTrue(chore.GetLastCompleted() >= before);
    }

    @Test
    public void Description_SeparatorsAreReplaced()
    {
        Chore chore = new Chore("Wash/dry; fold", Arrays.asList(WeekDay.Sunday), ChoreIconEnum.Sigh);
        assertEquals("Wash-dry, fold", chore.GetDescription());
        chore.SetDescription(" a/b ");
        assertEquals("a-b", chore.GetDescription());
        assertEquals("a-b", Chore.Parse(chore.ToSaveDataString()).GetDescription());
    }
}
