package com.zsoftware.choresrpg;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Shared setup for the app's activities: safe-area padding and the view switch buttons.
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
