package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;
import com.zsoftware.choresrpg.adapters.ChoreAdapter;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.FragmentChoreListBinding;
import com.zsoftware.choresrpg.databinding.WeekDayHeaderBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class ChoreListFragment extends Fragment {

    private FragmentChoreListBinding binding;
    private Dictionary<WeekDay, LinearLayout> ChoreLists;
    private Dictionary<WeekDay, WeekDayHeaderBinding> DayHeaders;

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

        DayHeaders = new Hashtable<WeekDay, WeekDayHeaderBinding>();
        DayHeaders.put(WeekDay.Monday, binding.mondayHeader);
        DayHeaders.put(WeekDay.Tuesday, binding.tuesdayHeader);
        DayHeaders.put(WeekDay.Wednesday, binding.wednesdayHeader);
        DayHeaders.put(WeekDay.Thursday, binding.thursdayHeader);
        DayHeaders.put(WeekDay.Friday, binding.fridayHeader);
        DayHeaders.put(WeekDay.Saturday, binding.saturdayHeader);
        DayHeaders.put(WeekDay.Sunday, binding.sundayHeader);

        for (WeekDay d: WeekDay.values())
        {
            WeekDay wd = d;
            DayHeaders.get(d).dayCard.setOnClickListener(view12 ->
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
    // and each day's header card (date, chore summary; today highlighted when the current week is shown).
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
            int done = 0;
            for (int i = 0; i < adapter.getCount(); i++)
            {
                Chore chore = adapter.getItem(i);
                if (chore.IsCompletedOn(date))
                    done++;
                View row = adapter.getView(i, null, list);
                row.setOnClickListener(rowView ->
                        startActivity(ChoreActivity.CreateIntent(requireContext(), chore)));
                list.addView(row);
            }
            ShowDayHeader(DayHeaders.get(d), d, date, d == selectedWeek, adapter.getCount(), done);
        }
    }

    // A day's header card: date badge, name, and e.g. "Today · 3 chores · 1 done". Today's badge is filled
    // and its outline thicker.
    private void ShowDayHeader(WeekDayHeaderBinding header, WeekDay day, Calendar date, boolean isToday, int choreCount, int doneCount)
    {
        String dayName = getString(FullDayNames[day.ordinal()]);
        header.dayNumber.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        header.dayName.setText(dayName);

        List<String> summary = new ArrayList<>();
        if (isToday)
            summary.add(getString(R.string.today));
        if (choreCount == 0)
            summary.add(getString(R.string.week_day_no_chores));
        else
        {
            summary.add(getResources().getQuantityString(R.plurals.week_day_chore_count, choreCount, choreCount));
            if (doneCount == choreCount)
                summary.add(getString(R.string.week_day_all_done));
            else if (doneCount > 0)
                summary.add(getResources().getQuantityString(R.plurals.week_day_done_count, doneCount, doneCount));
        }
        header.daySummary.setText(TextUtils.join(getString(R.string.week_day_summary_separator), summary));

        header.dayNumber.setBackgroundResource(isToday ? R.drawable.week_day_badge_today : R.drawable.week_day_badge);
        header.dayNumber.setTextColor(MaterialColors.getColor(header.dayNumber,
                isToday ? com.google.android.material.R.attr.colorOnPrimary : androidx.appcompat.R.attr.colorPrimary));
        header.dayCard.setStrokeWidth(getResources().getDimensionPixelSize(
                isToday ? R.dimen.week_day_header_stroke_today : R.dimen.week_day_header_stroke));
        header.dayCard.setContentDescription(getString(R.string.week_day_open, dayName + " " + date.get(Calendar.DAY_OF_MONTH)));
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