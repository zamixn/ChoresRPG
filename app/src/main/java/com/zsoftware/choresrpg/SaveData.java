package com.zsoftware.choresrpg;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    // (cloud.ChoreSync). Not called for changes that came from the cloud. Deleted chores are passed as
    // they were before deleting (their group says where the deletion goes).
    public interface SyncHandler
    {
        void ChoresSaved(List<Chore> changedChores, List<Chore> deletedChores);
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
        // What's left in 'saved' was deleted. Only chores in a group exist in the cloud.
        List<Chore> deleted = new ArrayList<>(saved.values());
        List<PendingDelete> pendingDeletes = LoadPendingDeletes();
        for (Chore chore : deleted)
        {
            if (chore.GetGroupId() != null)
                pendingDeletes.add(new PendingDelete(chore.GetId(), now, chore.GetGroupId()));
        }

        WriteChoreList(chores);
        WritePendingDeletes(pendingDeletes);
        if (Sync != null && (!changed.isEmpty() || !deleted.isEmpty()))
            Sync.ChoresSaved(changed, deleted);
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
    // with the time they were deleted and their group. Saved as "id:time:groupId;id:time:groupId"; entries
    // saved before groups existed have no group and count for every group.
    private static final String PendingDeletesKey = "PendingDeletes";
    private static class PendingDelete
    {
        final String Id;
        final long Time;
        final String GroupId;

        PendingDelete(String id, long time, String groupId)
        {
            Id = id;
            Time = time;
            GroupId = groupId;
        }

        boolean IsFor(String groupId) { return GroupId == null || GroupId.equals(groupId); }
    }

    private List<PendingDelete> LoadPendingDeletes()
    {
        List<PendingDelete> deletes = new ArrayList<>();
        String s = SharedPreferences.getString(PendingDeletesKey, "");
        if (s.isEmpty())
            return deletes;
        for (String entry : s.split(";"))
        {
            String[] parts = entry.split(":");
            if (parts.length >= 2)
                deletes.add(new PendingDelete(parts[0], Long.parseLong(parts[1]), parts.length > 2 ? parts[2] : null));
        }
        return deletes;
    }

    // The group's pending deletions: chore id -> time deleted.
    public Map<String, Long> GetPendingDeletes(String groupId)
    {
        Map<String, Long> deletes = new HashMap<>();
        for (PendingDelete delete : LoadPendingDeletes())
        {
            if (delete.IsFor(groupId))
                deletes.put(delete.Id, delete.Time);
        }
        return deletes;
    }

    // Called once the group in the cloud knows about the deletion.
    public void ClearPendingDelete(String groupId, String id)
    {
        List<PendingDelete> deletes = LoadPendingDeletes();
        boolean removed = false;
        for (int i = deletes.size() - 1; i >= 0; i--)
        {
            if (deletes.get(i).Id.equals(id) && deletes.get(i).IsFor(groupId))
            {
                deletes.remove(i);
                removed = true;
            }
        }
        if (removed)
            WritePendingDeletes(deletes);
    }

    private void WritePendingDeletes(List<PendingDelete> deletes)
    {
        StringBuilder s = new StringBuilder();
        for (PendingDelete delete : deletes)
        {
            if (s.length() > 0)
                s.append(';');
            s.append(delete.Id).append(':').append(delete.Time);
            if (delete.GroupId != null)
                s.append(':').append(delete.GroupId);
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

    // Removes every chore from this phone only (when signing out or deleting the account without keeping
    // them): nothing is sent to the cloud and no deletions are remembered for later. Stop syncing first.
    public void ClearLocalChores()
    {
        WriteChoreList(new ArrayList<>());
        WritePendingDeletes(new ArrayList<>());
    }

    // Removes a group's chores from this phone only (after leaving it or being removed from it): nothing is
    // sent to the cloud, so the group keeps them. Returns the removed chores.
    public List<Chore> RemoveGroupChores(String groupId)
    {
        List<Chore> kept = new ArrayList<>();
        List<Chore> removed = new ArrayList<>();
        for (Chore chore : GetChoreList())
        {
            if (groupId.equals(chore.GetGroupId()))
                removed.add(chore);
            else
                kept.add(chore);
        }
        List<PendingDelete> deletes = new ArrayList<>();
        for (PendingDelete delete : LoadPendingDeletes())
        {
            if (!groupId.equals(delete.GroupId))
                deletes.add(delete);
        }
        WriteChoreList(kept);
        WritePendingDeletes(deletes);
        return removed;
    }

    // The groups this phone last saw the user in (id -> name), to notice being removed from one while the
    // app was closed. Saved as "id:name;id:name" with the names URL-encoded.
    private static final String KnownGroupsKey = "KnownGroups";
    public Map<String, String> GetKnownGroups()
    {
        Map<String, String> groups = new LinkedHashMap<>();
        String s = SharedPreferences.getString(KnownGroupsKey, "");
        if (s.isEmpty())
            return groups;
        for (String entry : s.split(";"))
        {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2)
                groups.put(parts[0], Decode(parts[1]));
        }
        return groups;
    }

    public void SetKnownGroups(Map<String, String> groups)
    {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> group : groups.entrySet())
        {
            if (s.length() > 0)
                s.append(';');
            s.append(group.getKey()).append(':').append(Encode(group.getValue()));
        }
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putString(KnownGroupsKey, s.toString());
        editor.commit();
    }

    private static String Encode(String text)
    {
        try
        {
            return URLEncoder.encode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    private static String Decode(String text)
    {
        try
        {
            return URLDecoder.decode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException | IllegalArgumentException e)
        {
            return "";
        }
    }
}
