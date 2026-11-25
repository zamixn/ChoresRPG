package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DebugDataTest {

    @Test
    public void DebugChores_SurviveSaveFormatRoundTrip()
    {
        List<Chore> chores = DebugData.GetChores();
        assertFalse(chores.isEmpty());
        for (Chore chore : chores)
        {
            String saved = chore.ToSaveDataString();
            assertFalse("Separator in description: " + chore.GetDescription(),
                    chore.GetDescription().contains("/") || chore.GetDescription().contains(";"));

            Chore loaded = Chore.Parse(saved);
            assertEquals(chore.GetDescription(), loaded.GetDescription());
            assertEquals(chore.GetIcon(), loaded.GetIcon());
            assertEquals(chore.GetWeekDays(), loaded.GetWeekDays());
            assertEquals(chore.GetId(), loaded.GetId());
        }
    }

    @Test
    public void RandomChores_AreValidAndDifferent()
    {
        Random random = new Random(42);
        for (int run = 0; run < 50; run++)
        {
            List<Chore> chores = DebugData.GetRandomChores(random, 5);
            assertEquals(5, chores.size());
            Set<String> descriptions = new HashSet<>();
            for (Chore chore : chores)
            {
                descriptions.add(chore.GetDescription());
                assertFalse(chore.GetDescription().contains("/") || chore.GetDescription().contains(";"));
                assertNotNull(chore.GetIcon());
                assertTrue(chore.GetWeekDays().size() >= 1 && chore.GetWeekDays().size() <= 7);
                Chore loaded = Chore.Parse(chore.ToSaveDataString());
                assertEquals(chore.GetWeekDays(), loaded.GetWeekDays());
                assertEquals(chore.GetIcon(), loaded.GetIcon());
            }
            assertEquals("descriptions repeat", 5, descriptions.size());
        }
    }
}
