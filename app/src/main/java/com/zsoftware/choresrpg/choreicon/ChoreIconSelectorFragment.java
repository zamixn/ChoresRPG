package com.zsoftware.choresrpg.choreicon;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsoftware.choresrpg.databinding.FragmentChoreIconSelectorBinding;


public class ChoreIconSelectorFragment extends Fragment {

    private FragmentChoreIconSelectorBinding binding;
    public ChoreIconEnum ChoreIcon;
    public ChoreIconSelectorFragment() {}


    public static ChoreIconSelectorFragment newInstance() {
        ChoreIconSelectorFragment fragment = new ChoreIconSelectorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentChoreIconSelectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ChoreIcon = ChoreIconEnum.Sigh;
        binding.icon.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(ChoreIcon));
        binding.icon.setOnClickListener(view1 -> ShowSelectIconDialog());
        super.onViewCreated(view, savedInstanceState);
    }

    private void ShowSelectIconDialog()
    {
        ChoreIconPicker.Show(requireActivity(), ChoreIcon, this::OnIconSelected);
    }

    private void OnIconSelected(ChoreIconEnum icon)
    {
        ChoreIcon = icon;
        binding.icon.setImageDrawable(ChoreIconDictionary.Instance().GetIcon(icon));
    }
}