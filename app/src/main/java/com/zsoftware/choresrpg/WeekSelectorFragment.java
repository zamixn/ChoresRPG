package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.zsoftware.choresrpg.databinding.WeekSelectorFragmentBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// The add-chore form's weekday picker: a circle per day (tap to select / unselect; selected circles are
// filled), and quick picks for every day, weekdays (Monday - Friday) and the weekend.
public class WeekSelectorFragment extends Fragment {

    private static final String StateSelectedDays = "SelectedDays";

    private WeekSelectorFragmentBinding binding;

    private final List<WeekDay> SelectedWeekDays = new ArrayList<>();
    private final Map<WeekDay, TextView> WeekDayButtons = new EnumMap<>(WeekDay.class);

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = WeekSelectorFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WeekDayButtons.put(WeekDay.Monday, binding.buttonMonday);
        WeekDayButtons.put(WeekDay.Tuesday, binding.buttonTuesday);
        WeekDayButtons.put(WeekDay.Wednesday, binding.buttonWednesday);
        WeekDayButtons.put(WeekDay.Thursday, binding.buttonThursday);
        WeekDayButtons.put(WeekDay.Friday, binding.buttonFriday);
        WeekDayButtons.put(WeekDay.Saturday, binding.buttonSaturday);
        WeekDayButtons.put(WeekDay.Sunday, binding.buttonSunday);
        for (Map.Entry<WeekDay, TextView> entry : WeekDayButtons.entrySet())
        {
            WeekDay day = entry.getKey();
            entry.getValue().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) { ToggleDay(day); }
            });
        }

        binding.everyDayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SetSelection(Arrays.asList(WeekDay.values())); }
        });
        binding.weekdaysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetSelection(Arrays.asList(WeekDay.Monday, WeekDay.Tuesday, WeekDay.Wednesday, WeekDay.Thursday, WeekDay.Friday));
            }
        });
        binding.weekendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SetSelection(Arrays.asList(WeekDay.Saturday, WeekDay.Sunday)); }
        });

        // After a rotation: the days picked before it.
        if (savedInstanceState != null && savedInstanceState.getStringArrayList(StateSelectedDays) != null)
        {
            List<WeekDay> restored = new ArrayList<>();
            for (String day : savedInstanceState.getStringArrayList(StateSelectedDays))
                restored.add(WeekDay.valueOf(day));
            SetSelection(restored);
            PendingInitialSelection = null;
        }
        else if (PendingInitialSelection != null)
        {
            SetSelection(Collections.singletonList(PendingInitialSelection));
            PendingInitialSelection = null;
        }
        else
            ShowSelection();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> days = new ArrayList<>();
        for (WeekDay day : SelectedWeekDays)
            days.add(day.name());
        outState.putStringArrayList(StateSelectedDays, days);
    }

    // Selects a day when the selector first shows. Works before or after this fragment's view exists
    // (the parent form may call it before the selector's buttons are set up).
    private WeekDay PendingInitialSelection;
    public void SetInitialSelection(WeekDay weekDay)
    {
        if (binding != null)
        {
            if (!SelectedWeekDays.contains(weekDay))
                ToggleDay(weekDay);
        }
        else
            PendingInitialSelection = weekDay;
    }

    private void ToggleDay(WeekDay weekDay)
    {
        if (SelectedWeekDays.contains(weekDay))
            SelectedWeekDays.remove(weekDay);
        else
            SelectedWeekDays.add(weekDay);
        ShowSelection();
    }

    private void SetSelection(List<WeekDay> days)
    {
        SelectedWeekDays.clear();
        SelectedWeekDays.addAll(days);
        ShowSelection();
    }

    private void ShowSelection()
    {
        for (Map.Entry<WeekDay, TextView> entry : WeekDayButtons.entrySet())
            entry.getValue().setSelected(SelectedWeekDays.contains(entry.getKey()));
    }

    // The selected days, in week order.
    public List<WeekDay> GetSelectedWeekDays()
    {
        List<WeekDay> days = new ArrayList<>(SelectedWeekDays);
        Collections.sort(days);
        return days;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
