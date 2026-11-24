package com.zsoftware.choresrpg;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveData {
    private static String LOGTAG = "SaveData";
    private String SaveKey = "ChoreList";
    private static SaveData _Instance;
    public static SaveData Instance()
    {
        if (_Instance == null)
            Log.e(LOGTAG, "Instance: is null");
        return _Instance;
    }
    private SharedPreferences SharedPreferences;
    // Chores were first saved with Activity.getPreferences() from MainActivity, which uses a
    // preferences file named after that activity. Open that same file by name so every activity
    // (MainActivity, MonthActivity) reads and writes the same chore list.
    private static final String PreferencesFileName = "MainActivity";
    public static void Initialize(Context context)
    {
        _Instance = new SaveData();
        _Instance.SharedPreferences = context.getSharedPreferences(PreferencesFileName, MODE_PRIVATE);
        if (BuildConfig.DEBUG)
            _Instance.AddDebugDataOnce();
        // The simulated day (debug options) is kept between app starts; release builds use the real date.
        AppClock.SetDayOffset(BuildConfig.DEBUG ? _Instance.SharedPreferences.getInt(SimulatedDayOffsetKey, 0) : 0);
    }

    // Debug builds only: moves the whole app's "today" by this many days (see AppClock) and saves it.
    private static final String SimulatedDayOffsetKey = "SimulatedDayOffset";
    public void SetSimulatedDayOffset(int dayOffset)
    {
        AppClock.SetDayOffset(dayOffset);
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putInt(SimulatedDayOffsetKey, dayOffset);
        editor.commit();
    }

    // Debug builds only: appends the sample chores from DebugData to the saved chore list, once per
    // install. A flag remembers that it was done, so deleted debug chores don't come back.
    private String DebugDataAddedKey = "DebugDataAdded";
    private void AddDebugDataOnce()
    {
        if (SharedPreferences.getBoolean(DebugDataAddedKey, false))
            return;
        List<Chore> chores = GetChoreList();
        chores.addAll(DebugData.GetChores());
        SaveChoreList(chores);
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putBoolean(DebugDataAddedKey, true);
        editor.commit();
        Log.i(LOGTAG, "AddDebugDataOnce: added debug chores");
    }

    // Told about every change the user makes to the chores, so it can send them to the cloud
    // (cloud.ChoreSync). Not called for changes that came from the cloud.
    public interface SyncHandler
    {
        void ChoresSaved(List<Chore> changedChores, List<String> deletedChoreIds);
    }
    private static SyncHandler Sync;
    public static void SetSyncHandler(SyncHandler syncHandler) { Sync = syncHandler; }

    public List<Chore> GetChoreList()
    {
        List<Chore> chores = LoadChores();
        boolean addedIds = false;
        for (Chore chore : chores)
        {
            if (chore.WasLoadedWithoutId())
                addedIds = true;
        }
        // Chores saved before ids existed got new ids while loading; save them so the ids stay the same.
        if (addedIds)
            SaveChoreList(chores);
        return chores;
    }

    // The saved chores as they are (no saving back).
    private List<Chore> LoadChores()
    {
        ArrayList<Chore> chores = new ArrayList<Chore>();
        String s = SharedPreferences.getString(SaveKey, "");
        if(s.isEmpty())
            return chores;
        String[] strings = s.split(";");
        for (int i = 0; i < strings.length; i++)
        {
            Chore chore = Chore.Parse(strings[i]);
            if(chore != null)
                chores.add(chore);
            else
                Log.e(LOGTAG, String.format("LoadChores: Failed to parse %s", strings[i]));
        }
        return chores;
    }

    // Returns the chore with this id, or null if there is none (e.g. it was deleted).
    public Chore GetChore(String id)
    {
        for (Chore chore : GetChoreList())
        {
            if (chore.GetId().equals(id))
                return chore;
        }
        return null;
    }

    // Replaces the saved chore that has the same id.
    public void UpdateChore(Chore updated)
    {
        List<Chore> chores = GetChoreList();
        for (int i = 0; i < chores.size(); i++)
        {
            if (chores.get(i).GetId().equals(updated.GetId()))
            {
                chores.set(i, updated);
                SaveChoreList(chores);
                return;
            }
        }
        Log.e(LOGTAG, String.format("UpdateChore: no chore with id %s", updated.GetId()));
    }

    public void DeleteChore(String id)
    {
        List<Chore> chores = GetChoreList();
        for (int i = 0; i < chores.size(); i++)
        {
            if (chores.get(i).GetId().equals(id))
            {
                chores.remove(i);
                SaveChoreList(chores);
                return;
            }
        }
        Log.e(LOGTAG, String.format("DeleteChore: no chore with id %s", id));
    }
    public String[] GetChoreListAsStringArray()
    {
        ArrayList<Chore> chores = new ArrayList<Chore>();
        String s = SharedPreferences.getString(SaveKey, "");
        String[] strings = s.split(";");
        return strings;
    }

    // Saves the user's changes. Chores that differ from the saved ones get the current time as their
    // UpdatedAt, chores that are gone are remembered as deleted, and the sync handler is told about both.
    public void SaveChoreList(List<Chore> chores)
    {
        Map<String, Chore> saved = new HashMap<>();
        for (Chore chore : LoadChores())
        {
            // Old chores without ids got random ones while loading; they aren't real ids to compare against.
            if (!chore.WasLoadedWithoutId())
                saved.put(chore.GetId(), chore);
        }

        // Real time, not AppClock: it orders changes made on different devices.
        long now = System.currentTimeMillis();
        List<Chore> changed = new ArrayList<>();
        for (Chore chore : chores)
        {
            Chore before = saved.remove(chore.GetId());
            if (before == null || !before.HasSameContent(chore))
            {
                chore.SetUpdatedAt(now);
                changed.add(chore);
            }
        }
        // What's left in 'saved' was deleted.
        List<String> deletedIds = new ArrayList<>(saved.keySet());
        Map<String, Long> pendingDeletes = GetPendingDeletes();
        for (String id : deletedIds)
            pendingDeletes.put(id, now);

        WriteChoreList(chores);
        WritePendingDeletes(pendingDeletes);
        if (Sync != null && (!changed.isEmpty() || !deletedIds.isEmpty()))
            Sync.ChoresSaved(changed, deletedIds);
    }

    // Saves chores merged from the cloud: as they are, without new UpdatedAt times or telling the sync handler.
    public void SaveChoreListFromCloud(List<Chore> chores)
    {
        WriteChoreList(chores);
    }

    private void WriteChoreList(List<Chore> chores)
    {
        String s = "";
        for (int i = 0; i < chores.size(); i++)
        {
            Chore chore = chores.get(i);
            s += chore.ToSaveDataString();
            if(i < chores.size() - 1)
                s += ";";
        }
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putString(SaveKey, s);
        editor.commit();
    }

    // Chores deleted on this phone that the cloud doesn't know about yet (e.g. deleted while signed out),
    // with the time they were deleted. Saved as "id:time;id:time".
    private static final String PendingDeletesKey = "PendingDeletes";
    public Map<String, Long> GetPendingDeletes()
    {
        Map<String, Long> deletes = new HashMap<>();
        String s = SharedPreferences.getString(PendingDeletesKey, "");
        if (s.isEmpty())
            return deletes;
        for (String entry : s.split(";"))
        {
            String[] parts = entry.split(":");
            if (parts.length == 2)
                deletes.put(parts[0], Long.parseLong(parts[1]));
        }
        return deletes;
    }

    // Called once the cloud knows about the deletion.
    public void ClearPendingDelete(String id)
    {
        Map<String, Long> deletes = GetPendingDeletes();
        if (deletes.remove(id) != null)
            WritePendingDeletes(deletes);
    }

    private void WritePendingDeletes(Map<String, Long> deletes)
    {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, Long> entry : deletes.entrySet())
        {
            if (s.length() > 0)
                s.append(';');
            s.append(entry.getKey()).append(':').append(entry.getValue());
        }
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putString(PendingDeletesKey, s.toString());
        editor.commit();
    }

    // Deletes every chore (also from the cloud when signed in).
    public void DeleteAll()
    {
        SaveChoreList(new ArrayList<>());
    }
}
