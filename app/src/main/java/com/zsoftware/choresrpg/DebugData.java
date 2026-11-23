package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Sample chores added to the chore list in debug builds, so the list and calendar views have content
// to show. Descriptions must not contain '/' or ';' (the save format's separators).
public class DebugData {

    public static List<Chore> GetChores()
    {
        List<Chore> chores = new ArrayList<>();
        chores.add(new Chore("Wash the dishes", Arrays.asList(WeekDay.values()), ChoreIconEnum.Wash));
        chores.add(new Chore("Take out the trash", Arrays.asList(WeekDay.Monday, WeekDay.Thursday), ChoreIconEnum.Delete));
        chores.add(new Chore("Water the plants", Arrays.asList(WeekDay.Tuesday, WeekDay.Friday), ChoreIconEnum.LocalFlorist));
        chores.add(new Chore("Mop the floor", Arrays.asList(WeekDay.Wednesday, WeekDay.Saturday), ChoreIconEnum.CleaningServices));
        chores.add(new Chore("Vacuum the living room", Arrays.asList(WeekDay.Saturday), ChoreIconEnum.Weekend));
        chores.add(new Chore("Clean the fridge", Arrays.asList(WeekDay.Sunday), ChoreIconEnum.Kitchen));
        chores.add(new Chore("Do the laundry", Arrays.asList(WeekDay.Sunday), ChoreIconEnum.LocalLaundryService));
        return chores;
    }
}
