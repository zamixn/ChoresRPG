package com.zsoftware.choresrpg;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.zsoftware.choresrpg.choreicon.ChoreIconDictionary;
import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.databinding.FragmentChoreListBinding;

public class ChoreListFragment extends Fragment {

    private FragmentChoreListBinding binding;

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

        binding.addChoreFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ChoreListFragment.this)
                        .navigate(R.id.action_FirstFragment_to_addChoreFragment);
            }
        });
        UpdateChoreListView();
    }

    public void UpdateChoreListView()
    {
        String[] chores = SaveData.Instance().GetChoreListAsStringArray();
        ArrayAdapter<String> arrayAdapter =  new ArrayAdapter<String>(getContext(), R.layout.list_view_layout, chores);
        binding.choreListListView.setAdapter(arrayAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdateChoreListView();
    }
}