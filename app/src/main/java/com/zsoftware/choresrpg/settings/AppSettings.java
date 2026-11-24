package com.zsoftware.choresrpg.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// The user's settings. Saved in their own preferences file ("Settings"), separate from the chores
// (SaveData). Times are stored as minutes after midnight, local time.
public class AppSettings {
    private static String LOGTAG = "AppSettings";
    private static AppSettings _Instance;
    public static AppSettings Instance()
    {
        if (_Instance == null)
            Log.e(LOGTAG, "Instance: is null");
        return _Instance;
    }
    // Safe to call from every screen and from the notification alarms.
    public static void Initialize(Context context)
    {
        if (_Instance == null)
        {
            _Instance = new AppSettings();
            _Instance.SharedPreferences = context.getApplicationContext().getSharedPreferences(SettingsFileName, MODE_PRIVATE);
        }
    }

    private static final String SettingsFileName = "Settings";
    private static final String NotificationsEnabledKey = "NotificationsEnabled";
    private static final String MorningTimeKey = "MorningNotificationTime";
    private static final String EveningTimeKey = "EveningNotificationTime";

    public static final int DefaultMorningTime = 10 * 60;   // 10:00
    public static final int DefaultEveningTime = 20 * 60;   // 20:00

    private SharedPreferences SharedPreferences;

    // Send the daily chore notifications. Off by default.
    public boolean AreNotificationsEnabled() { return SharedPreferences.getBoolean(NotificationsEnabledKey, false); }
    public void SetNotificationsEnabled(boolean enabled) { Save(SharedPreferences.edit().putBoolean(NotificationsEnabledKey, enabled)); }

    // Morning notification time, in minutes after midnight.
    public int GetMorningTime() { return SharedPreferences.getInt(MorningTimeKey, DefaultMorningTime); }
    public void SetMorningTime(int minutes) { Save(SharedPreferences.edit().putInt(MorningTimeKey, minutes)); }

    // Evening (nightly) notification time, in minutes after midnight.
    public int GetEveningTime() { return SharedPreferences.getInt(EveningTimeKey, DefaultEveningTime); }
    public void SetEveningTime(int minutes) { Save(SharedPreferences.edit().putInt(EveningTimeKey, minutes)); }

    private void Save(SharedPreferences.Editor editor)
    {
        editor.commit();
    }
}
