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
import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.SaveData;
import com.zsoftware.choresrpg.ShortToast;
import com.zsoftware.choresrpg.WeekDay;
import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps the chores on this phone and the signed-in user's chores in Firestore the same.
// - Every change saved on the phone is sent right away (Firestore queues it while offline).
// - While signed in, changes in the cloud (from other devices) are merged into the phone's chores and
//   the open screen is told to refresh.
// The phone's chore list (SaveData) stays the app's working copy; signed out, nothing is synced.
//
// Firestore layout: users/{userId}/chores/{choreId} with description, icon, weekDays, completions,
// removedCompletions, updatedAt and deleted (deleted chores stay as markers so other devices delete them too).
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

    private static ListenerRegistration Registration;
    private static String ListeningUserId;
    // For the "couldn't sync" toast.
    private static Context AppContext;

    // Starts syncing for the signed-in user (stops when signed out). Safe to call from every screen;
    // SaveData must be initialized first.
    public static void Start(Context context)
    {
        AppContext = context.getApplicationContext();
        SaveData.SetSyncHandler(new SaveData.SyncHandler() {
            @Override
            public void ChoresSaved(List<Chore> changedChores, List<String> deletedChoreIds) {
                Upload(changedChores, deletedChoreIds);
            }
        });

        String userId = AccountManager.GetUserId();
        if (userId == null)
        {
            Stop();
            return;
        }
        if (userId.equals(ListeningUserId) && Registration != null)
            return;

        Stop();
        ListeningUserId = userId;
        // The first result has everything in the cloud; that's also when a first sign-in uploads the
        // phone's chores.
        Registration = GetChores(userId).addSnapshotListener((snapshot, error) -> {
            if (error != null)
            {
                // Firestore stops the listener after an error (e.g. PERMISSION_DENIED before the security
                // rules are published). Forget it, so the next screen that opens starts listening again.
                ReportFailure("Start: listening for cloud changes failed", error);
                Stop();
                return;
            }
            if (snapshot != null)
                OnCloudChores(snapshot);
        });
    }

    // Stops listening for cloud changes (after signing out). The phone keeps its chores.
    public static void Stop()
    {
        if (Registration != null)
            Registration.remove();
        Registration = null;
        ListeningUserId = null;
    }

    private static CollectionReference GetChores(String userId)
    {
        return FirebaseFirestore.getInstance().collection("users").document(userId).collection("chores");
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
    private static void ReportFailure(String message, Exception error)
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

    // Sends chores changed or deleted on this phone. Signed out, deletions wait in SaveData's pending list.
    private static void Upload(List<Chore> changedChores, List<String> deletedChoreIds)
    {
        String userId = AccountManager.GetUserId();
        SaveData saveData = SaveData.Instance();
        if (userId == null || saveData == null)
            return;
        CollectionReference chores = GetChores(userId);
        for (Chore chore : changedChores)
            Write(chores.document(chore.GetId()), ToDocument(chore), false);

        Map<String, Long> pendingDeletes = saveData.GetPendingDeletes();
        for (String id : deletedChoreIds)
        {
            Long deletedAt = pendingDeletes.get(id);
            UploadDelete(chores, id, deletedAt == null ? System.currentTimeMillis() : deletedAt);
        }
    }

    // Marks the chore deleted in the cloud (the document stays, so other devices see the deletion).
    // Firestore keeps the write even offline, so the phone no longer needs to remember it.
    private static void UploadDelete(CollectionReference chores, String id, long deletedAt)
    {
        Map<String, Object> deleted = new HashMap<>();
        deleted.put("deleted", true);
        deleted.put("updatedAt", deletedAt);
        Write(chores.document(id), deleted, true);
        SaveData.Instance().ClearPendingDelete(id);
    }

    private static void OnCloudChores(QuerySnapshot snapshot)
    {
        SaveData saveData = SaveData.Instance();
        String userId = AccountManager.GetUserId();
        if (saveData == null || userId == null)
            return;

        List<ChoreSyncMerge.Remote> remote = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments())
        {
            ChoreSyncMerge.Remote chore = FromDocument(document);
            if (chore != null)
                remote.add(chore);
        }

        ChoreSyncMerge.Result result = ChoreSyncMerge.Reconcile(saveData.GetChoreList(), remote, saveData.GetPendingDeletes());
        for (String id : result.PendingDeletesDone)
            saveData.ClearPendingDelete(id);
        if (result.LocalChanged)
        {
            saveData.SaveChoreListFromCloud(result.LocalChores);
            for (OnChoresChanged listener : new ArrayList<>(Listeners))
                listener.ChoresChanged();
        }

        CollectionReference chores = GetChores(userId);
        for (Chore chore : result.ToUpload)
            Write(chores.document(chore.GetId()), ToDocument(chore), false);
        for (Map.Entry<String, Long> delete : result.DeletesToUpload.entrySet())
            UploadDelete(chores, delete.getKey(), delete.getValue());
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
        document.put("completions", new ArrayList<>(chore.GetCompletionHistory()));
        document.put("removedCompletions", new ArrayList<>(chore.GetRemovedCompletions()));
        document.put("updatedAt", chore.GetUpdatedAt());
        document.put("deleted", false);
        return document;
    }

    // Null when the document can't be read (e.g. written by a newer app version with an unknown icon).
    private static ChoreSyncMerge.Remote FromDocument(DocumentSnapshot document)
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
