package com.zsoftware.choresrpg;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.choreicon.ChoreIconSelectorFragment;
import com.zsoftware.choresrpg.databinding.FragmentAddChoreFragmentBinding;
import com.zsoftware.choresrpg.databinding.FragmentChoreListBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddChoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddChoreFragment extends Fragment {

    private FragmentAddChoreFragmentBinding binding;

    public AddChoreFragment() {
        // Required empty public constructor
    }

    public static AddChoreFragment newInstance() {
        AddChoreFragment fragment = new AddChoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentAddChoreFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String desc = binding.choreDescription.getText().toString();
                List<WeekDay> weekdays = ((WeekSelectorFragment)binding.weekDaysFragmentContainerView.getFragment()).GetSelectedWeekDays();
                ChoreIconEnum icon = ((ChoreIconSelectorFragment)binding.iconFragmentContainerView.getFragment()).ChoreIcon;
                Chore c = new Chore(desc, weekdays, icon);

                List<Chore> chores = SaveData.Instance().GetChoreList();
                chores.add(c);
                SaveData.Instance().SaveChoreList(chores);

                Toast.makeText(getContext(), R.string.chore_saved, Toast.LENGTH_SHORT).show();

                NavHostFragment.findNavController(AddChoreFragment.this)
                        .navigate(R.id.action_addChoreFragment_to_FirstFragment);
            }
        });

    }
}