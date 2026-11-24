package com.zsoftware.choresrpg.cloud;

import com.zsoftware.choresrpg.Chore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Decides how the chores on this phone and the chores in the cloud come together. No Firebase code
// here, so it can be unit tested; ChoreSync feeds it and carries out the result.
//
// Rules:
// - A chore on only one side is copied to the other.
// - A chore on both sides is merged (Chore.Merge: newest edit wins, completions from both are kept).
// - A deleted chore stays deleted unless it was edited somewhere after it was deleted.
public class ChoreSyncMerge {

    // One chore document from the cloud. Chore is null when the document only marks a deletion.
    public static class Remote
    {
        public final String Id;
        public final Chore Chore;
        public final boolean Deleted;
        public final long UpdatedAt;

        public Remote(String id, Chore chore, boolean deleted, long updatedAt)
        {
            Id = id;
            Chore = chore;
            Deleted = deleted;
            UpdatedAt = updatedAt;
        }
    }

    public static class Result
    {
        // The phone's chores after merging (in the phone's order, new ones from the cloud at the end).
        public final List<Chore> LocalChores = new ArrayList<>();
        // True when LocalChores differs from what the phone had, so it has to be saved.
        public boolean LocalChanged = false;
        // Chores the cloud is missing or has an older / different version of.
        public final List<Chore> ToUpload = new ArrayList<>();
        // Deletions (id -> time deleted) the cloud doesn't know about yet.
        public final Map<String, Long> DeletesToUpload = new LinkedHashMap<>();
        // Pending deletions that no longer need sending (already deleted in the cloud, or never there).
        public final List<String> PendingDeletesDone = new ArrayList<>();
    }

    public static Result Reconcile(List<Chore> localChores, List<Remote> remoteChores, Map<String, Long> pendingDeletes)
    {
        Result result = new Result();
        LinkedHashMap<String, Chore> local = new LinkedHashMap<>();
        for (Chore chore : localChores)
            local.put(chore.GetId(), chore);
        Set<String> remoteIds = new HashSet<>();

        for (Remote remote : remoteChores)
        {
            remoteIds.add(remote.Id);
            Long deletedAt = pendingDeletes.get(remote.Id);
            if (deletedAt != null)
            {
                // Deleted on this phone, not yet in the cloud.
                if (remote.Deleted)
                    result.PendingDeletesDone.add(remote.Id);
                else if (remote.UpdatedAt > deletedAt && remote.Chore != null)
                {
                    // Edited on another device after this phone deleted it: keep the edit.
                    local.put(remote.Id, remote.Chore);
                    result.LocalChanged = true;
                    result.PendingDeletesDone.add(remote.Id);
                }
                else
                    result.DeletesToUpload.put(remote.Id, deletedAt);
                continue;
            }

            Chore mine = local.get(remote.Id);
            if (remote.Deleted)
            {
                if (mine == null)
                    continue;
                if (remote.UpdatedAt >= mine.GetUpdatedAt())
                {
                    local.remove(remote.Id);
                    result.LocalChanged = true;
                }
                else
                    result.ToUpload.add(mine);   // edited here after it was deleted elsewhere
                continue;
            }
            if (remote.Chore == null)
                continue;

            if (mine == null)
            {
                local.put(remote.Id, remote.Chore);
                result.LocalChanged = true;
                continue;
            }

            Chore merged = Chore.Merge(mine, remote.Chore);
            if (!IsSame(merged, mine))
            {
                local.put(remote.Id, merged);
                result.LocalChanged = true;
            }
            if (!IsSame(merged, remote.Chore))
                result.ToUpload.add(merged);
        }

        for (Chore chore : local.values())
        {
            if (!remoteIds.contains(chore.GetId()))
                result.ToUpload.add(chore);
        }
        for (String id : pendingDeletes.keySet())
        {
            if (!remoteIds.contains(id))
                result.PendingDeletesDone.add(id);
        }

        result.LocalChores.addAll(local.values());
        return result;
    }

    private static boolean IsSame(Chore a, Chore b)
    {
        return a.HasSameContent(b) && a.GetUpdatedAt() == b.GetUpdatedAt();
    }
}
