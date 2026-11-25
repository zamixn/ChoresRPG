package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public class Chore {
    // Unique id, generated when the chore is created. Used to find the chore in the saved list and as its
    // document id in the cloud; not shown in the UI.
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
    // Completions that were undone or moved. Kept so that syncing with another device doesn't bring
    // them back (the other device may still have them).
    private final List<Long> RemovedCompletions = new ArrayList<>();
    public List<Long> GetRemovedCompletions() { return Collections.unmodifiableList(RemovedCompletions); }
    // When the chore was last changed, in real milliseconds since the epoch (0 = unknown). Set by SaveData
    // when the chore is saved; when two devices changed the same chore, the newer change wins.
    private long UpdatedAt;
    public long GetUpdatedAt() { return UpdatedAt; }
    public void SetUpdatedAt(long updatedAt) { UpdatedAt = updatedAt; }
    // The group (cloud.GroupManager) the chore belongs to and is shared with; null for chores that were
    // never synced (signed out, or saved before groups existed). Those go into the main group once signed in.
    private String GroupId;
    public String GetGroupId() { return GroupId; }
    public void SetGroupId(String groupId) { GroupId = groupId; }

    // A copy for another group, with a new id (the original stays in its group, maybe on other phones too).
    public Chore CopyToGroup(String groupId)
    {
        Chore copy = FromCloud(UUID.randomUUID().toString(), Contents, Icon, WeekDays,
                CompletionHistory, RemovedCompletions, 0);
        copy.GroupId = groupId;
        return copy;
    }

    // The most recent completion; null if the chore was never completed.
    public Long GetLastCompleted()
    {
        return CompletionHistory.isEmpty() ? null : CompletionHistory.get(CompletionHistory.size() - 1);
    }
    // Moves the most recent completion to another time (adds one if there is none).
    public void SetLastCompleted(long lastCompleted)
    {
        RemoveLastCompletion();
        AddCompletion(lastCompleted);
    }
    // Undoes the most recent completion; the one before it (if any) becomes the last completed.
    public void RemoveLastCompletion()
    {
        if (CompletionHistory.isEmpty())
            return;
        RemoveCompletion(CompletionHistory.get(CompletionHistory.size() - 1));
    }
    // Undoes one completion (e.g. deleted from the history). Remembered as removed, so syncing doesn't bring it back.
    public void RemoveCompletion(long time)
    {
        if (!CompletionHistory.remove(Long.valueOf(time)))
            return;
        if (!RemovedCompletions.contains(time))
            RemovedCompletions.add(time);
    }
    // Undoes every completion on the same calendar day as the given date (e.g. "not done today after all").
    public void RemoveCompletionsOn(Calendar date)
    {
        Calendar completed = Calendar.getInstance(date.getTimeZone());
        for (Long time : new ArrayList<>(CompletionHistory))
        {
            completed.setTimeInMillis(time);
            if (completed.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                    && completed.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR))
                RemoveCompletion(time);
        }
    }
    public void Complete() { AddCompletion(AppClock.NowMillis()); }
    // Adds a completion at the given time (e.g. completing a missed chore on an earlier day).
    public void CompleteAt(long time) { AddCompletion(time); }

    private void AddCompletion(long time)
    {
        CompletionHistory.add(time);
        Collections.sort(CompletionHistory);
        RemovedCompletions.remove(Long.valueOf(time));
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

    // A chore as stored in the cloud (see cloud.ChoreSync).
    public static Chore FromCloud(String id, String description, ChoreIconEnum icon, List<WeekDay> weekDays,
                                  List<Long> completions, List<Long> removedCompletions, long updatedAt)
    {
        Chore c = new Chore(description, weekDays, icon);
        c.Id = id;
        c.CompletionHistory.addAll(completions);
        Collections.sort(c.CompletionHistory);
        c.RemovedCompletions.addAll(removedCompletions);
        c.UpdatedAt = updatedAt;
        return c;
    }

    // Combines two versions of the same chore (this phone's and the cloud's):
    // - description, icon and days come from the version changed last;
    // - completions from both are kept, except ones undone on either side.
    public static Chore Merge(Chore a, Chore b)
    {
        Chore newer = a.UpdatedAt >= b.UpdatedAt ? a : b;
        TreeSet<Long> removed = new TreeSet<>(a.RemovedCompletions);
        removed.addAll(b.RemovedCompletions);
        TreeSet<Long> completions = new TreeSet<>(a.CompletionHistory);
        completions.addAll(b.CompletionHistory);
        completions.removeAll(removed);
        Chore merged = FromCloud(newer.Id, newer.Contents, newer.Icon, newer.WeekDays,
                new ArrayList<>(completions), new ArrayList<>(removed), Math.max(a.UpdatedAt, b.UpdatedAt));
        merged.GroupId = a.GroupId != null ? a.GroupId : b.GroupId;
        return merged;
    }

    // True when both have the same data (ignoring when they were changed).
    public boolean HasSameContent(Chore other)
    {
        return ContentString().equals(other.ContentString()) && String.valueOf(GroupId).equals(String.valueOf(other.GroupId));
    }

    // '/' and ';' separate values in the save format, so they can't be stored in a description.
    public static String CleanDescription(String description)
    {
        if (description == null)
            return "";
        return description.replace('/', '-').replace(';', ',').trim();
    }

    // Save format: Description/Icon/<7 weekdays, "null" when not scheduled>/Id/LastCompleted/History/
    // RemovedCompletions/UpdatedAt/GroupId
    // History and RemovedCompletions are times separated by ',' ("null" when empty); GroupId is "null" when
    // there's none. Older data stops earlier: before ids existed after the weekdays, before the history
    // after LastCompleted, before syncing after the history, before groups after UpdatedAt.
    private static final int IdIndex = 9;
    private static final int LastCompletedIndex = 10;
    private static final int HistoryIndex = 11;
    private static final int RemovedCompletionsIndex = 12;
    private static final int UpdatedAtIndex = 13;
    private static final int GroupIdIndex = 14;
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
            c.CompletionHistory.addAll(ParseTimes(parts[HistoryIndex]));
            Collections.sort(c.CompletionHistory);
        }
        // Saved before the history existed: the last completion is the only one known.
        else if (parts.length > LastCompletedIndex && parts[LastCompletedIndex].compareTo("null") != 0)
            c.CompletionHistory.add(Long.parseLong(parts[LastCompletedIndex]));
        if (parts.length > RemovedCompletionsIndex && parts[RemovedCompletionsIndex].compareTo("null") != 0)
            c.RemovedCompletions.addAll(ParseTimes(parts[RemovedCompletionsIndex]));
        if (parts.length > UpdatedAtIndex)
            c.UpdatedAt = Long.parseLong(parts[UpdatedAtIndex]);
        if (parts.length > GroupIdIndex && !parts[GroupIdIndex].isEmpty() && parts[GroupIdIndex].compareTo("null") != 0)
            c.GroupId = parts[GroupIdIndex];
        return c;
    }
    public String ToSaveDataString()
    {
        return ContentString() + "/" + UpdatedAt + "/" + (GroupId == null ? "null" : GroupId);
    }

    // Everything in the save format except UpdatedAt.
    private String ContentString()
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
        s += "/" + (RemovedCompletions.isEmpty() ? "null" : JoinTimes(RemovedCompletions));
        return s;
    }

    private static List<Long> ParseTimes(String joined)
    {
        List<Long> times = new ArrayList<>();
        for (String time : joined.split(","))
            times.add(Long.parseLong(time));
        return times;
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
                ", RemovedCompletions=" + RemovedCompletions +
                ", UpdatedAt=" + UpdatedAt +
                ", GroupId=" + GroupId +
                '}';
    }
}
