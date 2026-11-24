package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    // Every time the chore was completed (chores repeat), in milliseconds since the epoch, oldest first.
    private final List<Long> CompletionHistory = new ArrayList<>();
    public List<Long> GetCompletionHistory() { return Collections.unmodifiableList(CompletionHistory); }
    // The most recent completion; null if the chore was never completed.
    public Long GetLastCompleted()
    {
        return CompletionHistory.isEmpty() ? null : CompletionHistory.get(CompletionHistory.size() - 1);
    }
    // Moves the most recent completion to another time (adds one if there is none).
    public void SetLastCompleted(long lastCompleted)
    {
        if (!CompletionHistory.isEmpty())
            CompletionHistory.remove(CompletionHistory.size() - 1);
        AddCompletion(lastCompleted);
    }
    // Undoes the most recent completion; the one before it (if any) becomes the last completed.
    public void RemoveLastCompletion()
    {
        if (!CompletionHistory.isEmpty())
            CompletionHistory.remove(CompletionHistory.size() - 1);
    }
    public void Complete() { AddCompletion(AppClock.NowMillis()); }

    private void AddCompletion(long time)
    {
        CompletionHistory.add(time);
        Collections.sort(CompletionHistory);
    }

    // True when the chore was completed (any time) on the same calendar day as the given date.
    public boolean IsCompletedOn(Calendar date)
    {
        Calendar completed = Calendar.getInstance(date.getTimeZone());
        for (Long time : CompletionHistory)
        {
            completed.setTimeInMillis(time);
            if (completed.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                    && completed.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR))
                return true;
        }
        return false;
    }
    public boolean IsCompletedToday() { return IsCompletedOn(AppClock.Now()); }

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

    // Save format: Description/Icon/<7 weekdays, "null" when not scheduled>/Id/LastCompleted/History
    // History is the completion times separated by ',' ("null" when empty). Older data stops earlier:
    // before ids existed after the weekdays, before the history after LastCompleted.
    private static final int IdIndex = 9;
    private static final int LastCompletedIndex = 10;
    private static final int HistoryIndex = 11;
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
        if (parts.length > HistoryIndex && parts[HistoryIndex].compareTo("null") != 0)
        {
            for (String time : parts[HistoryIndex].split(","))
                c.CompletionHistory.add(Long.parseLong(time));
            Collections.sort(c.CompletionHistory);
        }
        // Saved before the history existed: the last completion is the only one known.
        else if (parts.length > LastCompletedIndex && parts[LastCompletedIndex].compareTo("null") != 0)
            c.CompletionHistory.add(Long.parseLong(parts[LastCompletedIndex]));
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
        Long lastCompleted = GetLastCompleted();
        s += "/" + (lastCompleted == null ? "null" : lastCompleted.toString());
        s += "/" + (CompletionHistory.isEmpty() ? "null" : JoinTimes(CompletionHistory));
        return s;
    }

    private static String JoinTimes(List<Long> times)
    {
        StringBuilder joined = new StringBuilder();
        for (Long time : times)
        {
            if (joined.length() > 0)
                joined.append(',');
            joined.append(time);
        }
        return joined.toString();
    }

    @Override
    public String toString() {
        return "Chore{" +
                "Id='" + Id + '\'' +
                ", Contents='" + Contents + '\'' +
                ", WeekDays=" + WeekDays +
                ", Icon=" + Icon +
                ", CompletionHistory=" + CompletionHistory +
                '}';
    }
}
