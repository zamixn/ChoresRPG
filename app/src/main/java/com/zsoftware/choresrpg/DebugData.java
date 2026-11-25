package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Sample chores for debug builds, so the list and calendar views have content to show: a fixed set added
// once (GetChores), and random ones from the "dbg: add random chores" option (GetRandomChores).
// Descriptions must not contain '/' or ';' (the save format's separators).
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

    // Chore ideas for random chores: description and a matching icon.
    private static final String[] RandomDescriptions = {
            "Feed the cat", "Walk the dog", "Iron the shirts", "Clean the bathroom", "Scrub the bathtub",
            "Change the bed sheets", "Buy groceries", "Cook dinner", "Bake bread", "Pay the bills",
            "Recycle the bottles", "Mow the lawn", "Wash the car", "Clean the windows", "Dust the shelves",
            "Empty the dishwasher", "Fix the leaking tap", "Tidy the toys", "Take the medicine", "Check the mail",
            "Clean the oven", "Sort the pantry", "Fold the clothes", "Wipe the countertops", "Make the beds"
    };
    private static final ChoreIconEnum[] RandomIcons = {
            ChoreIconEnum.Pets, ChoreIconEnum.DirectionsWalk, ChoreIconEnum.Iron, ChoreIconEnum.Bathroom, ChoreIconEnum.Bathtub,
            ChoreIconEnum.Bed, ChoreIconEnum.LocalGroceryStore, ChoreIconEnum.DinnerDining, ChoreIconEnum.BakeryDining, ChoreIconEnum.Payments,
            ChoreIconEnum.Recycling, ChoreIconEnum.Grass, ChoreIconEnum.LocalCarWash, ChoreIconEnum.Window, ChoreIconEnum.Shelves,
            ChoreIconEnum.Flatware, ChoreIconEnum.Plumbing, ChoreIconEnum.Toys, ChoreIconEnum.Medication, ChoreIconEnum.Mail,
            ChoreIconEnum.Microwave, ChoreIconEnum.Inventory, ChoreIconEnum.Checkroom, ChoreIconEnum.Countertops, ChoreIconEnum.KingBed
    };

    // 'count' random chores (different descriptions while there are enough), each on 1 to 3 random weekdays,
    // or now and then every day.
    public static List<Chore> GetRandomChores(Random random, int count)
    {
        List<Integer> ideas = new ArrayList<>();
        for (int i = 0; i < RandomDescriptions.length; i++)
            ideas.add(i);
        Collections.shuffle(ideas, random);

        List<Chore> chores = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            int idea = ideas.get(i % ideas.size());
            chores.add(new Chore(RandomDescriptions[idea], GetRandomWeekDays(random), RandomIcons[idea]));
        }
        return chores;
    }

    private static List<WeekDay> GetRandomWeekDays(Random random)
    {
        if (random.nextInt(6) == 0)
            return Arrays.asList(WeekDay.values());
        List<WeekDay> days = new ArrayList<>(Arrays.asList(WeekDay.values()));
        Collections.shuffle(days, random);
        List<WeekDay> picked = new ArrayList<>(days.subList(0, 1 + random.nextInt(3)));
        Collections.sort(picked);   // week order, as the add-chore form saves them
        return picked;
    }
}
