package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.zsoftware.choresrpg.adapters.ChoreAdapter;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.FragmentChoreListBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class ChoreListFragment extends Fragment {

    private FragmentChoreListBinding binding;
    private Dictionary<WeekDay, LinearLayout> ChoreLists;
    private Dictionary<WeekDay, ImageView> WeekImages;
    private Dictionary<WeekDay, TextView> WeekTexts;

    private static final String StateWeekOffset = "WeekOffset";
    // Which week is shown: 0 = this week, -1 = last week, 1 = next week, ... (the arrows change it).
    private int WeekOffset = 0;

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

        WeekTexts = new Hashtable<WeekDay, TextView>();
        WeekTexts.put(WeekDay.Monday, binding.mondayText);
        WeekTexts.put(WeekDay.Tuesday, binding.tuesdayText);
        WeekTexts.put(WeekDay.Wednesday, binding.wednesdayText);
        WeekTexts.put(WeekDay.Thursday, binding.thursdayText);
        WeekTexts.put(WeekDay.Friday, binding.fridaytext);
        WeekTexts.put(WeekDay.Saturday, binding.saturdayText);
        WeekTexts.put(WeekDay.Sunday, binding.sundayText);

        for (WeekDay d: WeekDay.values())
        {
            WeekDay wd = d;
            WeekImages.get(d).setOnClickListener(view12 ->
                    startActivity(DayActivity.CreateIntent(requireContext(), wd, GetDate(wd))));
        }

        if (savedInstanceState != null)
            WeekOffset = savedInstanceState.getInt(StateWeekOffset, WeekOffset);
        ScreenSetup.SetupPeriodNavigator(binding.periodNavigator, R.string.previous_week, R.string.next_week,
                () -> ShowWeek(WeekOffset - 1), () -> ShowWeek(WeekOffset + 1), () -> ShowWeek(0));

        binding.addChoreFab.setOnClickListener(view1 ->
                NavHostFragment.findNavController(ChoreListFragment.this)
                .navigate(R.id.action_FirstFragment_to_addChoreFragment));
        UpdateChoreListViews();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(StateWeekOffset, WeekOffset);
    }

    private void ShowWeek(int weekOffset)
    {
        WeekOffset = weekOffset;
        UpdateChoreListViews();
        binding.weekScroll.scrollTo(0, 0);
    }

    // The date of a weekday in the shown week.
    private Calendar GetDate(WeekDay day)
    {
        Calendar date = day.GetDateThisWeek();
        date.add(Calendar.DAY_OF_MONTH, WeekOffset * 7);
        return date;
    }

    // e.g. "23 Nov – 29 Nov 2026"
    private String GetWeekLabel()
    {
        Calendar monday = GetDate(WeekDay.Monday);
        Calendar sunday = GetDate(WeekDay.Sunday);
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(monday.getTime())
                + " – " + new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(sunday.getTime());
    }

    // Fills every day's chore list for the shown week (all are always visible; the fragment scrolls),
    // puts the date in each day's header and highlights today when the current week is shown.
    public void UpdateChoreListViews()
    {
        WeekDay selectedWeek = WeekOffset == 0 ? GetCurrentWeekDay() : null;
        binding.periodNavigator.periodLabel.setText(GetWeekLabel());

        List<Chore> chores = SaveData.Instance().GetChoreList();
        for (WeekDay d: WeekDay.values())
        {
            // The lists sit inside a ScrollView, so they are plain LinearLayouts filled with one row per
            // chore (a ListView in a ScrollView only shows its first row).
            Calendar date = GetDate(d);
            ChoreAdapter adapter = new ChoreAdapter(getContext(), chores, d, date);
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

            // e.g. "Monday 23"
            WeekTexts.get(d).setText(getString(R.string.week_day_header, getString(FullDayNames[d.ordinal()]), date.get(Calendar.DAY_OF_MONTH)));

            ImageView iv = WeekImages.get(d);
            int box = d == selectedWeek ? R.drawable.rounded_box_selected : R.drawable.rounded_box;
            iv.setImageDrawable(ContextCompat.getDrawable(requireContext(), box));
        }
    }

    private WeekDay GetCurrentWeekDay()
    {
        Calendar calendar = AppClock.Now();
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
        UpdateChoreListViews();
        ChoreSync.AddListener(CloudChoresChanged);
        ScreenSetup.ShowRemovedFromGroupNotice(requireActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        ChoreSync.RemoveListener(CloudChoresChanged);
    }

    // Chores changed on another device: redraw.
    private final ChoreSync.OnChoresChanged CloudChoresChanged = new ChoreSync.OnChoresChanged() {
        @Override
        public void ChoresChanged() {
            if (binding != null)
                UpdateChoreListViews();
        }
    };

    private static final int[] FullDayNames = {
            R.string.week_day_button_monday_full,
            R.string.week_day_button_tuesday_full,
            R.string.week_day_button_wednesday_full,
            R.string.week_day_button_thursday_full,
            R.string.week_day_button_friday_full,
            R.string.week_day_button_saturday_full,
            R.string.week_day_button_sunday_full
    };
}