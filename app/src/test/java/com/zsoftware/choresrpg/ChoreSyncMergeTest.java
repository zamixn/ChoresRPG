package com.zsoftware.choresrpg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.cloud.ChoreSyncMerge;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChoreSyncMergeTest {

    private static Chore MakeChore(String id, String description, long updatedAt, Long... completions)
    {
        return Chore.FromCloud(id, description, ChoreIconEnum.Wash, Arrays.asList(WeekDay.Monday),
                Arrays.asList(completions), new ArrayList<>(), updatedAt);
    }

    private static ChoreSyncMerge.Remote Remote(Chore chore)
    {
        return new ChoreSyncMerge.Remote(chore.GetId(), chore, false, chore.GetUpdatedAt());
    }

    private static ChoreSyncMerge.Remote RemoteDeleted(String id, long deletedAt)
    {
        return new ChoreSyncMerge.Remote(id, null, true, deletedAt);
    }

    private static final Map<String, Long> NoPendingDeletes = new HashMap<>();

    // --- Chore.Merge ---

    @Test
    public void Merge_NewerEditWins_CompletionsFromBoth()
    {
        Chore mine = MakeChore("a", "Old name", 100, 1L, 2L);
        Chore theirs = MakeChore("a", "New name", 200, 2L, 3L);
        Chore merged = Chore.Merge(mine, theirs);
        assertEquals("New name", merged.GetDescription());
        assertEquals(Arrays.asList(1L, 2L, 3L), merged.GetCompletionHistory());
        assertEquals(200, merged.GetUpdatedAt());
    }

    @Test
    public void Merge_UndoneCompletionStaysUndone()
    {
        Chore mine = MakeChore("a", "Dishes", 100, 1L, 2L);
        Chore theirs = MakeChore("a", "Dishes", 200, 1L, 2L);
        theirs.RemoveLastCompletion();   // undo 2 on the other device
        Chore merged = Chore.Merge(mine, theirs);
        assertEquals(Collections.singletonList(1L), merged.GetCompletionHistory());
        assertEquals(Collections.singletonList(2L), merged.GetRemovedCompletions());
    }

    @Test
    public void SaveFormat_KeepsRemovedCompletionsAndUpdatedAt()
    {
        Chore chore = MakeChore("a", "Dishes", 12345, 1L, 2L);
        chore.RemoveLastCompletion();
        Chore loaded = Chore.Parse(chore.ToSaveDataString());
        assertTrue(chore.HasSameContent(loaded));
        assertEquals(12345, loaded.GetUpdatedAt());
        assertEquals(Collections.singletonList(2L), loaded.GetRemovedCompletions());
    }

    // --- Reconcile ---

    @Test
    public void FirstSignIn_LocalChoresUploaded_CloudChoresDownloaded()
    {
        Chore local = MakeChore("local", "Dishes", 100);
        Chore cloud = MakeChore("cloud", "Trash", 100);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                Arrays.asList(local), Arrays.asList(Remote(cloud)), NoPendingDeletes);
        assertTrue(result.LocalChanged);
        assertEquals(2, result.LocalChores.size());
        assertEquals(Collections.singletonList(local), result.ToUpload);
    }

    @Test
    public void SameOnBothSides_NothingToDo()
    {
        Chore local = MakeChore("a", "Dishes", 100, 5L);
        Chore cloud = MakeChore("a", "Dishes", 100, 5L);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                Arrays.asList(local), Arrays.asList(Remote(cloud)), NoPendingDeletes);
        assertFalse(result.LocalChanged);
        assertTrue(result.ToUpload.isEmpty());
        assertTrue(result.DeletesToUpload.isEmpty());
    }

    @Test
    public void BothChanged_MergedSavedLocallyAndUploaded()
    {
        Chore local = MakeChore("a", "Dishes", 100, 1L);
        Chore cloud = MakeChore("a", "Wash dishes", 200, 2L);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                Arrays.asList(local), Arrays.asList(Remote(cloud)), NoPendingDeletes);
        assertTrue(result.LocalChanged);
        Chore merged = result.LocalChores.get(0);
        assertEquals("Wash dishes", merged.GetDescription());
        assertEquals(Arrays.asList(1L, 2L), merged.GetCompletionHistory());
        // The cloud lacks completion 1, so the merge goes up too.
        assertEquals(1, result.ToUpload.size());
    }

    @Test
    public void DeletedInCloud_RemovedHere()
    {
        Chore local = MakeChore("a", "Dishes", 100);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                Arrays.asList(local), Arrays.asList(RemoteDeleted("a", 150)), NoPendingDeletes);
        assertTrue(result.LocalChanged);
        assertTrue(result.LocalChores.isEmpty());
    }

    @Test
    public void DeletedInCloud_ButEditedHereLater_KeptAndUploaded()
    {
        Chore local = MakeChore("a", "Dishes", 300);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                Arrays.asList(local), Arrays.asList(RemoteDeleted("a", 150)), NoPendingDeletes);
        assertFalse(result.LocalChanged);
        assertEquals(Collections.singletonList(local), result.ToUpload);
    }

    @Test
    public void DeletedHereWhileSignedOut_DeletionUploaded()
    {
        Chore cloud = MakeChore("a", "Dishes", 100);
        Map<String, Long> pending = new HashMap<>();
        pending.put("a", 200L);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                new ArrayList<>(), Arrays.asList(Remote(cloud)), pending);
        assertFalse(result.LocalChanged);
        assertEquals(Long.valueOf(200L), result.DeletesToUpload.get("a"));
    }

    @Test
    public void DeletedHere_ButEditedInCloudLater_KeptFromCloud()
    {
        Chore cloud = MakeChore("a", "Dishes", 300);
        Map<String, Long> pending = new HashMap<>();
        pending.put("a", 200L);
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(
                new ArrayList<>(), Arrays.asList(Remote(cloud)), pending);
        assertTrue(result.LocalChanged);
        assertEquals(1, result.LocalChores.size());
        assertTrue(result.DeletesToUpload.isEmpty());
        assertEquals(Collections.singletonList("a"), result.PendingDeletesDone);
    }

    @Test
    public void PendingDeleteNeverInCloud_Dropped()
    {
        Map<String, Long> pending = new HashMap<>();
        pending.put("gone", 200L);
        List<Chore> none = new ArrayList<>();
        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(none, new ArrayList<>(), pending);
        assertEquals(Collections.singletonList("gone"), result.PendingDeletesDone);
        assertTrue(result.DeletesToUpload.isEmpty());
    }
}
