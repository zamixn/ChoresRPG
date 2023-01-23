package com.zsoftware.choresrpg;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.zsoftware.choresrpg.databinding.WeekSelectorFragmentBinding;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class WeekSelectorFragment extends Fragment {

    private WeekSelectorFragmentBinding binding;

    private List<WeekDay> SelectedWeekDays;
    private Dictionary<WeekDay, Button> WeekDayButtons;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        SelectedWeekDays = new ArrayList<>();
        WeekDayButtons = new Hashtable<>();
        binding = WeekSelectorFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("TAG", "weekDay.toString()");

        WeekDayButtons.put(WeekDay.Monday, binding.buttonMonday);
        binding.buttonMonday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Monday); }
        });
        WeekDayButtons.put(WeekDay.Tuesday, binding.buttonTuesday);
        binding.buttonTuesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Tuesday); }
        });
        WeekDayButtons.put(WeekDay.Wednesday, binding.buttonWednesday);
        binding.buttonWednesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Wednesday); }
        });
        WeekDayButtons.put(WeekDay.Thursday, binding.buttonThursday);
        binding.buttonThursday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Thursday); }
        });
        WeekDayButtons.put(WeekDay.Friday, binding.buttonFriday);
        binding.buttonFriday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Friday); }
        });
        WeekDayButtons.put(WeekDay.Saturday, binding.buttonSaturday);
        binding.buttonSaturday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Saturday); }
        });
        WeekDayButtons.put(WeekDay.Sunday, binding.buttonSunday);
        binding.buttonSunday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { SelectDay(WeekDay.Sunday); }
        });
    }

    private void SelectDay(WeekDay weekDay)
    {
        Button b = WeekDayButtons.get(weekDay);
        if(!SelectedWeekDays.contains(weekDay))
        {
            SelectedWeekDays.add(weekDay);
            b.setBackground(getResources().getDrawable(R.drawable.round_button_selected));
        }
        else
        {
            SelectedWeekDays.remove(weekDay);
            b.setBackground(getResources().getDrawable(R.drawable.round_button));
        }
    }

    public List<WeekDay> GetSelectedWeekDays()
    {
        return SelectedWeekDays;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
