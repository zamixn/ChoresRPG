package com.zsoftware.choresrpg;

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
