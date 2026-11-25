package com.zsoftware.choresrpg;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsoftware.choresrpg.choreicon.ChoreIconEnum;
import com.zsoftware.choresrpg.choreicon.ChoreIconSelectorFragment;
import com.zsoftware.choresrpg.cloud.AccountManager;
import com.zsoftware.choresrpg.cloud.Group;
import com.zsoftware.choresrpg.cloud.GroupManager;
import com.zsoftware.choresrpg.group.GroupDialogs;
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

    // Optional argument: name of a WeekDay that starts out selected (set when adding from the day view).
    public static final String ArgSelectedDay = "SelectedDay";

    private FragmentAddChoreFragmentBinding binding;
    // The group the new chore goes into (null while signed out or before the groups are known).
    private String GroupId;
    private static final String StateGroupId = "GroupId";

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(StateGroupId, GroupId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // The group row shows only while signed in with the groups known; one group needs no choosing.
    private void ShowGroup()
    {
        Group group = GroupManager.GetGroup(GroupId);
        boolean show = AccountManager.IsSignedIn() && group != null;
        binding.groupSection.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show)
            return;
        binding.groupName.setText(group.Name);
        boolean canChoose = GroupManager.GetGroups().size() > 1;
        binding.changeGroupButton.setVisibility(canChoose ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (savedInstanceState == null && args != null && args.getString(ArgSelectedDay) != null)
        {
            WeekDay selectedDay = WeekDay.valueOf(args.getString(ArgSelectedDay));
            ((WeekSelectorFragment)binding.weekDaysFragmentContainerView.getFragment()).SetInitialSelection(selectedDay);
        }

        if (savedInstanceState != null)
            GroupId = savedInstanceState.getString(StateGroupId);
        else
            GroupId = GroupManager.GetMainGroupId();
        ShowGroup();
        View.OnClickListener chooseGroup = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GroupManager.GetGroups().size() <= 1)
                    return;
                GroupDialogs.ChooseGroup(requireActivity(), R.string.group_choose, GroupId, new GroupDialogs.OnGroupChosen() {
                    @Override
                    public void GroupChosen(String groupId) {
                        GroupId = groupId;
                        ShowGroup();
                    }
                });
            }
        };
        binding.groupRow.setOnClickListener(chooseGroup);
        // Typing again clears the "can't be empty" error.
        binding.choreDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.descriptionLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        binding.changeGroupButton.setOnClickListener(chooseGroup);

        binding.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String desc = Chore.CleanDescription(String.valueOf(binding.choreDescription.getText()));
                if (desc.isEmpty())
                {
                    binding.descriptionLayout.setError(getString(R.string.description_empty));
                    binding.choreDescription.requestFocus();
                    return;
                }
                List<WeekDay> weekdays = ((WeekSelectorFragment)binding.weekDaysFragmentContainerView.getFragment()).GetSelectedWeekDays();
                ChoreIconEnum icon = ((ChoreIconSelectorFragment)binding.iconFragmentContainerView.getFragment()).ChoreIcon;
                Chore c = new Chore(desc, weekdays, icon);
                // Signed out (or groups not loaded yet): no group; it goes into the main group later.
                c.SetGroupId(GroupId);

                List<Chore> chores = SaveData.Instance().GetChoreList();
                chores.add(c);
                SaveData.Instance().SaveChoreList(chores);

                ShortToast.Show(getContext(), R.string.chore_saved);

                // Opened from the month or day view: close the form and return there.
                if (getActivity() instanceof AddChoreActivity)
                {
                    getActivity().finish();
                    return;
                }
                NavHostFragment.findNavController(AddChoreFragment.this)
                        .navigate(R.id.action_addChoreFragment_to_FirstFragment);
            }
        });

    }
}