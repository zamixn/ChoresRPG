package com.zsoftware.choresrpg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zsoftware.choresrpg.databinding.PeriodNavigatorBinding;
import com.zsoftware.choresrpg.ftue.FtueActivity;
import com.zsoftware.choresrpg.settings.SettingsActivity;
import com.zsoftware.choresrpg.reminders.ChoreReminders;

import java.text.DateFormat;

// Shared setup for the app's screens: safe-area padding, the view switch buttons and the
// previous / next bar.
public class ScreenSetup {
    private static final int INSET_TYPES = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();

    // Keeps the toolbar below the status bar / camera cutout and the screen content
    // (including buttons at the bottom) above the navigation bar and the keyboard.
    public static void ApplySafeAreaInsets(View appBar, View content)
    {
        ViewCompat.setOnApplyWindowInsetsListener(appBar, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
                Insets bars = windowInsets.getInsets(INSET_TYPES);
                view.setPadding(bars.left, bars.top, bars.right, 0);
                return windowInsets;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(content, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
                Insets bars = windowInsets.getInsets(INSET_TYPES);
                Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                view.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
                return windowInsets;
            }
        });
    }

    // For a screen without a toolbar (the first-time setup): keeps all of it inside the status bar,
    // navigation bar, camera cutout and keyboard.
    public static void ApplySafeAreaInsetsToScreen(View screen)
    {
        ViewCompat.setOnApplyWindowInsetsListener(screen, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
                Insets bars = windowInsets.getInsets(INSET_TYPES);
                Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                return windowInsets;
            }
        });
    }

    // The header (toolbar) items shared by the daily, weekly and monthly views: the three view buttons,
    // Settings, Delete All Save Data and the debug options. Returns true when the item was handled.
    // 'redraw' runs after all chores were deleted.
    public static boolean HandleHeaderMenuItem(Activity activity, int itemId, Runnable redraw)
    {
        if (HandleDebugNotificationItem(activity, itemId) || HandleDebugFtueItem(activity, itemId))
            return true;
        // Debug: "today" moved; rebuild the screen so every date shown follows it.
        if (HandleDebugMenuItem(activity, itemId))
        {
            activity.recreate();
            return true;
        }
        if (itemId == R.id.action_daily_view)
            OpenDailyView(activity);
        else if (itemId == R.id.action_weekly_view)
            OpenWeeklyView(activity);
        else if (itemId == R.id.action_monthly_view)
            OpenMonthlyView(activity);
        else if (itemId == R.id.action_settings)
            activity.startActivity(new Intent(activity, SettingsActivity.class));
        else if (itemId == R.id.action_delete_all_save_data)
        {
            SaveData.Instance().DeleteAll();
            ShortToast.Show(activity, R.string.all_data_deleted);
            redraw.run();
        }
        else
            return false;
        return true;
    }

    // Switching between the daily, weekly and monthly views: the weekly view (MainActivity) always stays
    // at the bottom; the daily or monthly view opens on top of it, replacing the view that was open.
    public static void OpenDailyView(Activity from)
    {
        from.startActivity(DayActivity.CreateDailyViewIntent(from));
        if (!(from instanceof MainActivity))
            from.finish();
    }

    public static void OpenWeeklyView(Activity from)
    {
        if (from instanceof MainActivity)
            return;
        // Brings the existing weekly view back instead of starting a new one, closing what's above it.
        Intent weekly = new Intent(from, MainActivity.class);
        weekly.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(weekly);
        from.finish();
    }

    public static void OpenMonthlyView(Activity from)
    {
        from.startActivity(new Intent(from, MonthActivity.class));
        if (!(from instanceof MainActivity))
            from.finish();
    }

    // Debug option "show first-time setup": opens the FTUE again. Returns true when the item was this one.
    public static boolean HandleDebugFtueItem(Activity activity, int itemId)
    {
        if (itemId != R.id.action_show_ftue)
            return false;
        activity.startActivity(FtueActivity.CreateIntent(activity));
        return true;
    }

    // For a floating button placed directly in the screen's CoordinatorLayout (outside the padded
    // content): moves it up and in by the navigation bar / cutout, on top of its own margins.
    public static void ApplyFabInsets(View fab)
    {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        final int baseBottom = params.bottomMargin;
        final int baseEnd = params.getMarginEnd();
        ViewCompat.setOnApplyWindowInsetsListener(fab, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
                Insets bars = windowInsets.getInsets(INSET_TYPES);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                lp.bottomMargin = baseBottom + bars.bottom;
                lp.setMarginEnd(baseEnd + Math.max(bars.right, bars.left));
                view.setLayoutParams(lp);
                return windowInsets;
            }
        });
    }

    // Wires the previous / next bar (period_navigator.xml) of the month, week and day views.
    // Tapping the label goes back to the current period.
    public static void SetupPeriodNavigator(PeriodNavigatorBinding navigator, int previousDescription, int nextDescription,
                                            Runnable onPrevious, Runnable onNext, Runnable onCurrent)
    {
        Context context = navigator.getRoot().getContext();
        navigator.previousButton.setContentDescription(context.getString(previousDescription));
        navigator.nextButton.setContentDescription(context.getString(nextDescription));
        navigator.previousButton.setOnClickListener(view -> onPrevious.run());
        navigator.nextButton.setOnClickListener(view -> onNext.run());
        navigator.periodLabelArea.setOnClickListener(view -> onCurrent.run());
    }

    // Debug builds: shows the "simulate next / previous day" options in the ⋮ menu ("back to the real
    // day" only while a day is simulated).
    public static void SetupDebugMenu(Menu menu)
    {
        menu.setGroupVisible(R.id.debug_options, BuildConfig.DEBUG);
        MenuItem realDay = menu.findItem(R.id.action_simulate_real_day);
        if (realDay != null)
            realDay.setVisible(BuildConfig.DEBUG && AppClock.GetDayOffset() != 0);
    }

    // Debug option "send chore reminder notification": shows today's chore notification right away
    // (whether or not notifications are turned on in the settings). Returns true when the item was this one.
    public static boolean HandleDebugNotificationItem(Activity activity, int itemId)
    {
        if (itemId != R.id.action_send_reminder_notification)
            return false;
        if (!ChoreReminders.CanPostNotifications(activity))
            ShortToast.Show(activity, R.string.notifications_blocked);
        else if (!ChoreReminders.ShowTodayNotification(activity, ChoreReminders.Morning))
            ShortToast.Show(activity, R.string.reminder_no_chores_today);
        return true;
    }

    // Handles the debug day options: moves the app's "today" (AppClock) and shows the new date.
    // Returns true when the item was one of them; the screen then has to redraw itself.
    public static boolean HandleDebugMenuItem(Activity activity, int itemId)
    {
        int dayOffset = AppClock.GetDayOffset();
        if (itemId == R.id.action_simulate_next_day)
            dayOffset++;
        else if (itemId == R.id.action_simulate_previous_day)
            dayOffset--;
        else if (itemId == R.id.action_simulate_real_day)
            dayOffset = 0;
        else
            return false;

        SaveData.Instance().SetSimulatedDayOffset(dayOffset);
        String today = DateFormat.getDateInstance(DateFormat.FULL).format(AppClock.Now().getTime());
        ShortToast.Show(activity, activity.getString(R.string.simulated_day, today));
        return true;
    }

    // Dims and disables the toolbar button of the view that is already open.
    public static void MarkCurrentView(Menu menu, int currentViewItemId)
    {
        MenuItem item = menu.findItem(currentViewItemId);
        if (item == null)
            return;
        item.setEnabled(false);
        if (item.getIcon() != null)
            item.setIcon(item.getIcon().mutate()).getIcon().setAlpha(110);
    }
}
