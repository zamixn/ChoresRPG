package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class Chore {
    // Unique id, generated when the chore is created. Used to find the chore in the saved list; not shown in the UI.
    private String Id;
    public String GetId() { return Id; }
    private String Contents;
    public String GetDescription() { return Contents; }
    public void SetDescription(String description) { Contents = CleanDescription(description); }
    private List<WeekDay> WeekDays;
    public List<WeekDay> GetWeekDays() { return WeekDays; }
    public void SetWeekDays(List<WeekDay> weekDays) { WeekDays = new ArrayList<>(weekDays); }
    public Boolean IsOnDay(WeekDay day) { return WeekDays.contains(day); }
    private ChoreIconEnum Icon;
    public ChoreIconEnum GetIcon() { return Icon; }
    public void SetIcon(ChoreIconEnum icon) { Icon = icon; }
    // When the chore was last completed, in milliseconds since the epoch; null if it never was.
    private Long LastCompleted;
    public Long GetLastCompleted() { return LastCompleted; }
    public void SetLastCompleted(Long lastCompleted) { LastCompleted = lastCompleted; }
    public void Complete() { LastCompleted = System.currentTimeMillis(); }

    // True when the chore was last completed on the same calendar day as the given date.
    public boolean IsCompletedOn(Calendar date)
    {
        if (LastCompleted == null)
            return false;
        Calendar completed = Calendar.getInstance(date.getTimeZone());
        completed.setTimeInMillis(LastCompleted);
        return completed.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                && completed.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR);
    }
    public boolean IsCompletedToday() { return IsCompletedOn(Calendar.getInstance()); }

    // True when this chore was loaded from data saved before chores had ids; the loaded chore got a
    // new id, and the list has to be saved again so the id sticks.
    private boolean LoadedWithoutId;
    public boolean WasLoadedWithoutId() { return LoadedWithoutId; }

    public Chore(String contents) {
        Id = UUID.randomUUID().toString();
        Contents = CleanDescription(contents);
        WeekDays = new ArrayList<>();
    }
    public Chore(String contents, List<WeekDay> weekdays, ChoreIconEnum icon) {
        Id = UUID.randomUUID().toString();
        Contents = CleanDescription(contents);
        WeekDays = new ArrayList(weekdays);
        Icon = icon;
    }

    // '/' and ';' separate values in the save format, so they can't be stored in a description.
    public static String CleanDescription(String description)
    {
        if (description == null)
            return "";
        return description.replace('/', '-').replace(';', ',').trim();
    }

    // Save format: Description/Icon/<7 weekdays, "null" when not scheduled>/Id/LastCompleted
    // Data saved before ids existed stops after the weekdays.
    private static final int IdIndex = 9;
    private static final int LastCompletedIndex = 10;
    public static Chore Parse(String s)
    {
        String[] parts = s.split("/");
        Chore c = new Chore(parts[0]);
        c.Icon = ChoreIconEnum.FromSavedName(parts[1]);
        for (int i = 2; i < 9 && i < parts.length; i++)
        {
            String w = parts[i];
            if(w.compareTo("null") == 0)
                continue;
            c.WeekDays.add(WeekDay.valueOf(w));
        }
        if (parts.length > IdIndex && !parts[IdIndex].isEmpty())
            c.Id = parts[IdIndex];
        else
            c.LoadedWithoutId = true;
        if (parts.length > LastCompletedIndex && parts[LastCompletedIndex].compareTo("null") != 0)
            c.LastCompleted = Long.parseLong(parts[LastCompletedIndex]);
        return c;
    }
    public String ToSaveDataString()
    {
        String s = Contents + "/" + Icon.toString();
        for (WeekDay w : WeekDay.values()) {
            if(WeekDays.contains(w))
                s += "/" + w.toString();
            else
                s += "/null";
        }
        s += "/" + Id;
        s += "/" + (LastCompleted == null ? "null" : LastCompleted.toString());
        return s;
    }

    @Override
    public String toString() {
        return "Chore{" +
                "Id='" + Id + '\'' +
                ", Contents='" + Contents + '\'' +
                ", WeekDays=" + WeekDays +
                ", Icon=" + Icon +
                ", LastCompleted=" + LastCompleted +
                '}';
    }
}
