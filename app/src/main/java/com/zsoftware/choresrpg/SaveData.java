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
    public static void Initialize(Activity context)
    {
        _Instance = new SaveData();
        _Instance.SharedPreferences = context.getPreferences(MODE_PRIVATE);
    }

    public List<Chore> GetChoreList()
    {
        ArrayList<Chore> chores = new ArrayList<Chore>();
        String s = SharedPreferences.getString(SaveKey, "");
        if(s == "")
            return chores;
        String[] strings = s.split(";");
        for (int i = 0; i < strings.length; i++)
        {
            Chore chore = Chore.Parse(strings[i]);
            if(chore != null)
                chores.add(chore);
            else
                Log.e(LOGTAG, String.format("GetChoreList: Failed to parse %s", strings[i]));
        }
        return chores;
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
