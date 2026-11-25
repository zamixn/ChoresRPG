package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zsoftware.choresrpg.cloud.GroupManager;

import org.junit.Test;

public class InviteCodeTest {

    @Test
    public void PlainCode()
    {
        assertEquals("ab23cd45ef67", GroupManager.ParseInviteCode("ab23cd45ef67"));
    }

    @Test
    public void TypedWithSpacesDashesAndCapitals()
    {
        assertEquals("ab23cd45ef67", GroupManager.ParseInviteCode("  AB23-CD45 EF67 "));
    }

    @Test
    public void PastedLink()
    {
        assertEquals("ab23cd45ef67", GroupManager.ParseInviteCode(GroupManager.GetInviteLink("ab23cd45ef67")));
    }

    @Test
    public void NothingUsable()
    {
        assertNull(GroupManager.ParseInviteCode(null));
        assertNull(GroupManager.ParseInviteCode("   "));
        assertNull(GroupManager.ParseInviteCode("choresrpg://join/"));
        assertNull(GroupManager.ParseInviteCode("ab#cd"));
    }
}
