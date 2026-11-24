package com.zsoftware.choresrpg.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Alarms are lost on reboot and move when the clock or time zone changes (and after an app update),
// so the notification alarms are set again on those system events.
public class ReminderRescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
            ChoreReminders.Schedule(context);
    }
}
