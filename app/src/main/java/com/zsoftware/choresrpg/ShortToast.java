package com.zsoftware.choresrpg;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

// Toasts that disappear after about a second. Android's shortest toast (LENGTH_SHORT) stays for
// about 2 seconds, so the toast is cancelled early.
public class ShortToast {
    private static final long DurationMillis = 1000;

    public static void Show(Context context, int textId)
    {
        // Application context: the toast may outlive the screen that showed it (e.g. after deleting a chore).
        Toast toast = Toast.makeText(context.getApplicationContext(), textId, Toast.LENGTH_SHORT);
        toast.show();
        new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, DurationMillis);
    }
}
