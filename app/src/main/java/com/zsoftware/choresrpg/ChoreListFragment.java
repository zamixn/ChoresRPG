package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.zsoftware.choresrpg.adapters.ChoreAdapter;
import com.zsoftware.choresrpg.databinding.FragmentChoreListBinding;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ChoreListFragment extends Fragment {

    private FragmentChoreListBinding binding;
    private Dictionary<WeekDay, ListView> ChoreLists;
    private Dictionary<WeekDay, ImageView> WeekImages;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentChoreListBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChoreLists = new Hashtable<WeekDay, ListView>();
        ChoreLists.put(WeekDay.Monday, binding.mondayChoreListListView);
        ChoreLists.put(WeekDay.Tuesday, binding.tuesdayChoreListListView);
        ChoreLists.put(WeekDay.Wednesday, binding.wednesdayChoreListListView);
        ChoreLists.put(WeekDay.Thursday, binding.thursdayChoreListListView);
        ChoreLists.put(WeekDay.Friday, binding.fridayChoreListListView);
        ChoreLists.put(WeekDay.Saturday, binding.saturdayChoreListListView);
        ChoreLists.put(WeekDay.Sunday, binding.sundayChoreListListView);

        WeekImages = new Hashtable<WeekDay, ImageView>();
        WeekImages.put(WeekDay.Monday, binding.mondayImageView);
        WeekImages.put(WeekDay.Tuesday, binding.tuesdayImageView);
        WeekImages.put(WeekDay.Wednesday, binding.wednesdayImageView);
        WeekImages.put(WeekDay.Thursday, binding.thursdayImageView);
        WeekImages.put(WeekDay.Friday, binding.fridayImageView);
        WeekImages.put(WeekDay.Saturday, binding.saturdayImageView);
        WeekImages.put(WeekDay.Sunday, binding.sundayImageView);

        for (WeekDay d: WeekDay.values())
        {
            WeekDay wd = d;
            WeekImages.get(d).setOnClickListener(view12 -> UpdateChoreListViews(wd));
        }

        binding.addChoreFab.setOnClickListener(view1 ->
                NavHostFragment.findNavController(ChoreListFragment.this)
                .navigate(R.id.action_FirstFragment_to_addChoreFragment));
        UpdateChoreListViews(GetCurrentWeekDay());
    }

    public void UpdateChoreListViews(WeekDay selectedWeek)
    {
        List<Chore> chores = SaveData.Instance().GetChoreList();
        for (WeekDay d: WeekDay.values())
        {
            ChoreAdapter arrayAdapter =  new ChoreAdapter(getContext(), chores, d);
            ListView lv = ChoreLists.get(d);
            lv.setAdapter(arrayAdapter);
            lv.setVisibility(View.GONE);

            ImageView iv = WeekImages.get(d);
            iv.setImageDrawable(getResources().getDrawable(R.drawable.rounded_box));
        }

        ListView currentDayView = ChoreLists.get(selectedWeek);
        currentDayView.setVisibility(View.VISIBLE);
        ImageView currentImageView = WeekImages.get(selectedWeek);
        currentImageView.setImageDrawable(getResources().getDrawable(R.drawable.rounded_box_selected));
    }

    private WeekDay GetCurrentWeekDay()
    {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        switch (day)
        {
            case Calendar.MONDAY: return WeekDay.Monday;
            case Calendar.TUESDAY: return WeekDay.Tuesday;
            case Calendar.WEDNESDAY: return WeekDay.Wednesday;
            case Calendar.THURSDAY: return WeekDay.Thursday;
            case Calendar.FRIDAY: return WeekDay.Friday;
            case Calendar.SATURDAY: return WeekDay.Saturday;
            case Calendar.SUNDAY: return WeekDay.Sunday;
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdateChoreListViews(GetCurrentWeekDay());
    }
}