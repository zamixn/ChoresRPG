package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.ActivityAddChoreBinding;

// Shows the add-chore form (AddChoreFragment) for the month and day views. The week view shows the
// same form inside MainActivity's navigation instead. Closes itself after the chore is saved.
public class AddChoreActivity extends AppCompatActivity {

    private ActivityAddChoreBinding binding;

    // No day selected by default (month view).
    public static Intent CreateIntent(Context context)
    {
        return new Intent(context, AddChoreActivity.class);
    }

    // With this weekday already selected in the form (day view).
    public static Intent CreateIntent(Context context, WeekDay selectedDay)
    {
        Intent intent = CreateIntent(context);
        intent.putExtra(AddChoreFragment.ArgSelectedDay, selectedDay.name());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start(this);
        ChoreIconDictionary.Initialize(this);

        binding = ActivityAddChoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.addChoreContainer);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Added once; after a rotation the fragment manager restores it.
        if (savedInstanceState == null)
        {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.add_chore_container, AddChoreFragment.class, getIntent().getExtras())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
