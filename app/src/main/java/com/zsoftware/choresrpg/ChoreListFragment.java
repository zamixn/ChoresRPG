package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private Dictionary<WeekDay, LinearLayout> ChoreLists;
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

        ChoreLists = new Hashtable<WeekDay, LinearLayout>();
        ChoreLists.put(WeekDay.Monday, binding.mondayChoreList);
        ChoreLists.put(WeekDay.Tuesday, binding.tuesdayChoreList);
        ChoreLists.put(WeekDay.Wednesday, binding.wednesdayChoreList);
        ChoreLists.put(WeekDay.Thursday, binding.thursdayChoreList);
        ChoreLists.put(WeekDay.Friday, binding.fridayChoreList);
        ChoreLists.put(WeekDay.Saturday, binding.saturdayChoreList);
        ChoreLists.put(WeekDay.Sunday, binding.sundayChoreList);

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
            WeekImages.get(d).setOnClickListener(view12 ->
                    startActivity(DayActivity.CreateIntent(requireContext(), wd)));
        }

        binding.addChoreFab.setOnClickListener(view1 ->
                NavHostFragment.findNavController(ChoreListFragment.this)
                .navigate(R.id.action_FirstFragment_to_addChoreFragment));
        UpdateChoreListViews(GetCurrentWeekDay());
    }

    // Fills every day's chore list (all are always visible; the fragment scrolls) and highlights the selected day.
    public void UpdateChoreListViews(WeekDay selectedWeek)
    {
        List<Chore> chores = SaveData.Instance().GetChoreList();
        for (WeekDay d: WeekDay.values())
        {
            // The lists sit inside a ScrollView, so they are plain LinearLayouts filled with one row per
            // chore (a ListView in a ScrollView only shows its first row).
            ChoreAdapter adapter = new ChoreAdapter(getContext(), chores, d, d.GetDateThisWeek());
            LinearLayout list = ChoreLists.get(d);
            list.removeAllViews();
            for (int i = 0; i < adapter.getCount(); i++)
            {
                Chore chore = adapter.getItem(i);
                View row = adapter.getView(i, null, list);
                row.setOnClickListener(rowView ->
                        startActivity(ChoreActivity.CreateIntent(requireContext(), chore)));
                list.addView(row);
            }

            ImageView iv = WeekImages.get(d);
            int box = d == selectedWeek ? R.drawable.rounded_box_selected : R.drawable.rounded_box;
            iv.setImageDrawable(ContextCompat.getDrawable(requireContext(), box));
        }
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