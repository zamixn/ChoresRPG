package com.zsoftware.choresrpg;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    public static void Initialize(Activity context)
    {
        _Instance = new SaveData();
        _Instance.SharedPreferences = context.getSharedPreferences(PreferencesFileName, MODE_PRIVATE);
        if (BuildConfig.DEBUG)
            _Instance.AddDebugDataOnce();
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

    public List<Chore> GetChoreList()
    {
        ArrayList<Chore> chores = new ArrayList<Chore>();
        String s = SharedPreferences.getString(SaveKey, "");
        if(s.isEmpty())
            return chores;
        String[] strings = s.split(";");
        boolean addedIds = false;
        for (int i = 0; i < strings.length; i++)
        {
            Chore chore = Chore.Parse(strings[i]);
            if(chore != null)
            {
                chores.add(chore);
                if (chore.WasLoadedWithoutId())
                    addedIds = true;
            }
            else
                Log.e(LOGTAG, String.format("GetChoreList: Failed to parse %s", strings[i]));
        }
        // Chores saved before ids existed got new ids while loading; save them so the ids stay the same.
        if (addedIds)
            SaveChoreList(chores);
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

    public void SaveChoreList(List<Chore> chores)
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

    public void DeleteAll()
    {
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putString(SaveKey, "");
        editor.commit();
    }
}
