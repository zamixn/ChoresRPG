package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.List;

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
        }
    }
}
