package com.zsoftware.choresrpg;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.ftue.FtueActivity;
import com.zsoftware.choresrpg.reminders.ChoreReminders;
import com.zsoftware.choresrpg.settings.AppSettings;
import com.zsoftware.choresrpg.settings.SettingsActivity;
import com.zsoftware.choresrpg.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Draw behind the system bars on every Android version (Android 15+ forces this anyway).
        // The status bar uses light icons because the app bar behind it is dark.
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);
        ChoreIconDictionary.Initialize(this);
        // Make sure the daily notification alarms are set (they are lost e.g. when the app is force-stopped).
        ChoreReminders.Schedule(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentMain.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // App start: the daily view opens on top of this week view (it's the default view); on the very first
        // start the first-time setup comes first and opens the daily view when finished. Only when opened
        // from the launcher: not after a rotation, and not when coming back from a notification's day view.
        AppSettings.Initialize(this);
        boolean openedFromLauncher = Intent.ACTION_MAIN.equals(getIntent().getAction());
        if (savedInstanceState == null && openedFromLauncher)
        {
            if (!AppSettings.Instance().IsFtueCompleted())
                startActivity(FtueActivity.CreateIntent(this));
            else
                startActivity(DayActivity.CreateDailyViewIntent(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        ScreenSetup.MarkCurrentView(menu, R.id.action_weekly_view);
        ScreenSetup.SetupDebugMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // View buttons, Settings, Delete All Save Data and debug options (shared with the other views).
        if (ScreenSetup.HandleHeaderMenuItem(this, item.getItemId(), this::recreate))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}