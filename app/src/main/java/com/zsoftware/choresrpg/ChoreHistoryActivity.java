package com.zsoftware.choresrpg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.cloud.ChoreSync;
import com.zsoftware.choresrpg.databinding.ActivityChoreHistoryBinding;
import com.zsoftware.choresrpg.databinding.ChoreHistoryItemBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// ChoreHistory: every past completion of one chore, newest first. Opened from the chore view.
public class ChoreHistoryActivity extends AppCompatActivity {

    private static final String ExtraChoreId = "ChoreId";

    private ActivityChoreHistoryBinding binding;
    private String ChoreId;

    public static Intent CreateIntent(Context context, Chore chore)
    {
        Intent intent = new Intent(context, ChoreHistoryActivity.class);
        intent.putExtra(ExtraChoreId, chore.GetId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        SaveData.Initialize(this);
        ChoreSync.Start();
        ChoreIconDictionary.Initialize(this);

        binding = ActivityChoreHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.ApplySafeAreaInsets(binding.appBar, binding.contentHistory);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.chore_history);

        ChoreId = getIntent().getStringExtra(ExtraChoreId);
        binding.historyList.setEmptyView(binding.noHistoryText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadHistory();
        ChoreSync.AddListener(CloudChoresChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChoreSync.RemoveListener(CloudChoresChanged);
    }

    // Chores changed on another device: show the new history.
    private final ChoreSync.OnChoresChanged CloudChoresChanged = new ChoreSync.OnChoresChanged() {
        @Override
        public void ChoresChanged() {
            LoadHistory();
        }
    };

    private void LoadHistory()
    {
        Chore chore = SaveData.Instance().GetChore(ChoreId);
        if (chore == null)
        {
            ShortToast.Show(this, R.string.chore_not_found);
            finish();
            return;
        }

        binding.toolbar.setSubtitle(chore.GetDescription());
        List<Long> completions = new ArrayList<>(chore.GetCompletionHistory());
        Collections.reverse(completions);
        binding.historyCount.setText(getResources().getQuantityString(
                R.plurals.completed_times, completions.size(), completions.size()));
        binding.historyList.setAdapter(new CompletionAdapter(completions));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // One row per completion: the date on the first line, the time on the second.
    private class CompletionAdapter extends BaseAdapter {
        private final List<Long> Completions;
        private final DateFormat DayFormat = DateFormat.getDateInstance(DateFormat.FULL);
        private final DateFormat TimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        CompletionAdapter(List<Long> completions)
        {
            Completions = completions;
        }

        @Override
        public int getCount() {
            return Completions.size();
        }

        @Override
        public Long getItem(int position) {
            return Completions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChoreHistoryItemBinding row;
            if (convertView == null)
            {
                row = ChoreHistoryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                row.getRoot().setTag(row);
            }
            else
                row = (ChoreHistoryItemBinding) convertView.getTag();

            Date completed = new Date(getItem(position));
            row.completionDate.setText(DayFormat.format(completed));
            row.completionTime.setText(TimeFormat.format(completed));
            return row.getRoot();
        }
    }
}
