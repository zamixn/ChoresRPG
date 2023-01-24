package com.zsoftware.choresrpg;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;

import java.util.ArrayList;
import java.util.List;

public class Chore {
    private String Contents;
    public String GetDescription() { return Contents; }
    private List<WeekDay> WeekDays;
    public List<WeekDay> GetWeekDays() { return WeekDays; }
    private ChoreIconEnum Icon;
    public ChoreIconEnum GetIcon() { return Icon; }

    public Chore(String contents) {
        Contents = contents;
        WeekDays = new ArrayList<>();
    }
    public Chore(String contents, List<WeekDay> weekdays, ChoreIconEnum icon) {
        Contents = contents;
        WeekDays = new ArrayList(weekdays);
        Icon = icon;
    }

    public static Chore Parse(String s)
    {
        String[] parts = s.split("/");
        Chore c = new Chore(parts[0]);
        c.Icon = ChoreIconEnum.valueOf(parts[1]);
        for (int i = 2; i < 9 && i < parts.length; i++)
        {
            String w = parts[i];
            if(w.compareTo("null") == 0)
                continue;
            c.WeekDays.add(WeekDay.valueOf(w));
        }
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
        return s;
    }

    @Override
    public String toString() {
        return "Chore{" +
                "Contents='" + Contents + '\'' +
                ", WeekDays=" + WeekDays +
                ", Icon=" + Icon +
                '}';
    }
}
