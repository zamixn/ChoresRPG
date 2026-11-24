package com.zsoftware.choresrpg;

import android.app.Activity;
import android.content.Context;
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
        navigator.periodLabel.setOnClickListener(view -> onCurrent.run());
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
