package com.zsoftware.choresrpg.cloud;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.zsoftware.choresrpg.Chore;
import com.zsoftware.choresrpg.ChoreSchedule;
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.WeekDay;
import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Keeps the chores on this phone and the chores of the signed-in user's groups in Firestore the same.
// - Every change saved on the phone is sent right away to the chore's group (Firestore queues it while
//   offline).
// - While signed in, changes in the cloud (from other devices and other members) are merged into the
//   phone's chores and the open screen is told to refresh.
// The phone's chore list (SaveData) stays the app's working copy and holds the chores of every group
// (each chore knows its group); signed out, nothing is synced.
//
// GroupManager finds the user's groups and tells ChoreSync which ones to sync (SyncGroups).
// Firestore layout: groups/{groupId}/chores/{choreId} with description, icon, weekDays, repeat,
// repeatInterval, monthDays, startDate (the schedule), completions, removedCompletions, updatedAt and deleted (deleted chores stay as markers so other devices delete them too).
public class ChoreSync {
    private static String LOGTAG = "ChoreSync";

    // Screens showing chores register while visible, to redraw when chores arrive from the cloud.
    public interface OnChoresChanged
    {
        void ChoresChanged();
    }
    private static final List<OnChoresChanged> Listeners = new ArrayList<>();
    public static void AddListener(OnChoresChanged listener) { Listeners.add(listener); }
    public static void RemoveListener(OnChoresChanged listener) { Listeners.remove(listener); }
    // Tells the visible screen the phone's chores changed (e.g. a group's chores were removed).
    static void NotifyChoresChanged()
    {
        for (OnChoresChanged listener : new ArrayList<>(Listeners))
            listener.ChoresChanged();
    }

    // Group id -> the listener on that group's chores.
    private static final Map<String, ListenerRegistration> Registrations = new HashMap<>();
    // For the "couldn't sync" toast.
    private static Context AppContext;

    // Starts syncing the chores of the signed-in user's groups (stops when signed out). Safe to call from
    // every screen; SaveData must be initialized first. The groups are looked up (or created) first.
    public static void Start(Context context)
    {
        AppContext = context.getApplicationContext();
        SaveData.SetSyncHandler(new SaveData.SyncHandler() {
            @Override
            public void ChoresSaved(List<Chore> changedChores, List<Chore> deletedChores) {
                Upload(changedChores, deletedChores);
            }
        });

        if (AccountManager.GetUserId() == null)
        {
            Stop();
            return;
        }
        GroupManager.Start(AppContext);
    }

    // Syncs exactly these groups: starts listening to new ones, stops listening to the rest.
    static void SyncGroups(Collection<String> groupIds)
    {
        for (String groupId : new ArrayList<>(Registrations.keySet()))
        {
            if (!groupIds.contains(groupId))
                StopGroup(groupId);
        }
        for (String groupId : groupIds)
        {
            if (!Registrations.containsKey(groupId))
                ListenTo(groupId);
        }
    }

    private static void ListenTo(String groupId)
    {
        // The first result has everything in the cloud; that's also when chores only on the phone go up.
        Registrations.put(groupId, GetChores(groupId).addSnapshotListener((snapshot, error) -> {
            if (error != null)
            {
                // Firestore stops the listener after an error. Forget it, so it's started again the next time
                // a screen opens. PERMISSION_DENIED means the user is no longer in the group; GroupManager
                // notices that by itself, so no toast.
                StopGroup(groupId);
                if (error.getCode() != FirebaseFirestoreException.Code.PERMISSION_DENIED)
                    ReportFailure("ListenTo: listening for cloud changes failed", error);
                return;
            }
            if (snapshot != null)
                OnCloudChores(groupId, snapshot);
        }));
    }

    // Stops syncing one group (left it, or removed from it). The phone keeps whatever it has.
    static void StopGroup(String groupId)
    {
        ListenerRegistration registration = Registrations.remove(groupId);
        if (registration != null)
            registration.remove();
    }

    // Stops listening for cloud changes (after signing out). The phone keeps its chores.
    public static void Stop()
    {
        for (ListenerRegistration registration : Registrations.values())
            registration.remove();
        Registrations.clear();
    }

    private static CollectionReference GetChores(String groupId)
    {
        return FirebaseFirestore.getInstance().collection("groups").document(groupId).collection("chores");
    }

    // Writes a chore document. Offline, Firestore queues it (that's not a failure); the toast is only
    // for writes the cloud refuses (e.g. security rules).
    private static void Write(DocumentReference document, Map<String, Object> data, boolean merge)
    {
        Task<Void> write = merge ? document.set(data, SetOptions.merge()) : document.set(data);
        write.addOnFailureListener(error -> ReportFailure("Write: couldn't save chore " + document.getId(), error));
    }

    // Logs a sync failure and shows a short "couldn't sync" toast; at most one toast every few seconds,
    // since one failure often hits many chores at once.
    private static final long FailureToastIntervalMillis = 10000;
    private static long LastFailureToastMillis = 0;
    static void ReportFailure(String message, Exception error)
    {
        Log.e(LOGTAG, message, error);
        long now = System.currentTimeMillis();
        if (AppContext == null || now - LastFailureToastMillis < FailureToastIntervalMillis)
            return;
        LastFailureToastMillis = now;
        boolean refused = error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
        ShortToast.Show(AppContext, refused ? R.string.sync_refused : R.string.sync_failed);
    }

    // Sends chores changed or deleted on this phone to their groups. For groups not synced right now
    // (signed out, or still being looked up), deletions wait in SaveData's pending list and changes go up
    // with the group's next sync.
    private static void Upload(List<Chore> changedChores, List<Chore> deletedChores)
    {
        SaveData saveData = SaveData.Instance();
        if (AccountManager.GetUserId() == null || saveData == null)
            return;
        for (Chore chore : changedChores)
        {
            String groupId = chore.GetGroupId();
            if (groupId != null && Registrations.containsKey(groupId))
                Write(GetChores(groupId).document(chore.GetId()), ToDocument(chore), false);
        }

        for (Chore chore : deletedChores)
        {
            String groupId = chore.GetGroupId();
            if (groupId == null || !Registrations.containsKey(groupId))
                continue;
            Long deletedAt = saveData.GetPendingDeletes(groupId).get(chore.GetId());
            UploadDelete(groupId, chore.GetId(), deletedAt == null ? System.currentTimeMillis() : deletedAt);
        }
    }

    // Marks the chore deleted in the cloud (the document stays, so other devices see the deletion).
    // Firestore keeps the write even offline, so the phone no longer needs to remember it.
    private static void UploadDelete(String groupId, String id, long deletedAt)
    {
        Map<String, Object> deleted = new HashMap<>();
        deleted.put("deleted", true);
        deleted.put("updatedAt", deletedAt);
        Write(GetChores(groupId).document(id), deleted, true);
        SaveData.Instance().ClearPendingDelete(groupId, id);
    }

    // Merges one group's chores from the cloud with that group's chores on the phone; the other groups'
    // chores stay as they are.
    private static void OnCloudChores(String groupId, QuerySnapshot snapshot)
    {
        SaveData saveData = SaveData.Instance();
        if (saveData == null || !Registrations.containsKey(groupId) || AccountManager.GetUserId() == null)
            return;

        List<ChoreSyncMerge.Remote> remote = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments())
        {
            ChoreSyncMerge.Remote chore = FromDocument(groupId, document);
            if (chore != null)
                remote.add(chore);
        }

        List<Chore> allChores = saveData.GetChoreList();
        List<Chore> groupChores = new ArrayList<>();
        for (Chore chore : allChores)
        {
            if (groupId.equals(chore.GetGroupId()))
                groupChores.add(chore);
        }

        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(groupChores, remote, saveData.GetPendingDeletes(groupId));
        for (String id : result.PendingDeletesDone)
            saveData.ClearPendingDelete(groupId, id);
        if (result.LocalChanged)
        {
            saveData.SaveChoreListFromCloud(ReplaceGroupChores(allChores, groupId, result.LocalChores));
            NotifyChoresChanged();
        }

        for (Chore chore : result.ToUpload)
            Write(GetChores(groupId).document(chore.GetId()), ToDocument(chore), false);
        for (Map.Entry<String, Long> delete : result.DeletesToUpload.entrySet())
            UploadDelete(groupId, delete.getKey(), delete.getValue());
    }

    // The phone's chores with one group's chores replaced by 'groupChores', keeping the phone's order
    // (new ones at the end).
    private static List<Chore> ReplaceGroupChores(List<Chore> allChores, String groupId, List<Chore> groupChores)
    {
        Map<String, Chore> replacements = new LinkedHashMap<>();
        for (Chore chore : groupChores)
            replacements.put(chore.GetId(), chore);
        List<Chore> merged = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Chore chore : allChores)
        {
            if (!groupId.equals(chore.GetGroupId()))
            {
                merged.add(chore);
                ids.add(chore.GetId());
                continue;
            }
            Chore replacement = replacements.remove(chore.GetId());
            if (replacement != null)
            {
                merged.add(replacement);
                ids.add(replacement.GetId());
            }
        }
        for (Chore chore : replacements.values())
        {
            // Ids are unique across groups; never show the same chore twice.
            if (ids.add(chore.GetId()))
                merged.add(chore);
        }
        return merged;
    }

    private static Map<String, Object> ToDocument(Chore chore)
    {
        List<String> weekDays = new ArrayList<>();
        for (WeekDay day : chore.GetWeekDays())
            weekDays.add(day.name());
        Map<String, Object> document = new HashMap<>();
        document.put("description", chore.GetDescription());
        document.put("icon", chore.GetIcon().name());
        document.put("weekDays", weekDays);
        ChoreSchedule schedule = chore.GetSchedule();
        document.put("repeat", schedule.GetRepeat().name());
        document.put("repeatInterval", schedule.GetInterval());
        document.put("monthDays", new ArrayList<>(schedule.GetMonthDays()));
        document.put("startDate", schedule.GetStartDate());
        document.put("completions", new ArrayList<>(chore.GetCompletionHistory()));
        document.put("removedCompletions", new ArrayList<>(chore.GetRemovedCompletions()));
        document.put("updatedAt", chore.GetUpdatedAt());
        document.put("deleted", false);
        return document;
    }

    // Null when the document can't be read (e.g. written by a newer app version with an unknown icon).
    private static ChoreSyncMerge.Remote FromDocument(String groupId, DocumentSnapshot document)
    {
        try
        {
            boolean deleted = Boolean.TRUE.equals(document.getBoolean("deleted"));
            Long updatedAt = document.getLong("updatedAt");
            long updated = updatedAt == null ? 0 : updatedAt;
            if (deleted)
                return new ChoreSyncMerge.Remote(document.getId(), null, true, updated);

            List<WeekDay> weekDays = new ArrayList<>();
            for (Object day : AsList(document.get("weekDays")))
                weekDays.add(WeekDay.valueOf(String.valueOf(day)));
            Chore chore = Chore.FromCloud(document.getId(),
                    document.getString("description"),
                    ChoreIconEnum.FromSavedName(document.getString("icon")),
                    weekDays,
                    AsTimes(document.get("completions")),
                    AsTimes(document.get("removedCompletions")),
                    updated);
            chore.SetGroupId(groupId);
            // Saved before monthly / every-n schedules existed: weekly, every week.
            ChoreSchedule schedule = chore.GetSchedule();
            String repeat = document.getString("repeat");
            if (repeat != null)
                schedule.SetRepeat(ChoreSchedule.RepeatKind.valueOf(repeat));
            Long interval = document.getLong("repeatInterval");
            if (interval != null)
                schedule.SetInterval(interval.intValue());
            List<Integer> monthDays = new ArrayList<>();
            for (Object day : AsList(document.get("monthDays")))
            {
                if (day instanceof Number)
                    monthDays.add(((Number) day).intValue());
            }
            schedule.SetMonthDays(monthDays);
            Long startDate = document.getLong("startDate");
            if (startDate != null)
                schedule.SetStartDate(startDate.intValue());
            chore.SetSchedule(schedule);
            return new ChoreSyncMerge.Remote(document.getId(), chore, false, updated);
        }
        catch (RuntimeException e)
        {
            Log.e(LOGTAG, "FromDocument: can't read chore " + document.getId(), e);
            return null;
        }
    }

    private static List<?> AsList(Object value)
    {
        return value instanceof List ? (List<?>) value : new ArrayList<>();
    }

    private static List<Long> AsTimes(Object value)
    {
        List<Long> times = new ArrayList<>();
        for (Object time : AsList(value))
        {
            if (time instanceof Number)
                times.add(((Number) time).longValue());
        }
        return times;
    }
}
