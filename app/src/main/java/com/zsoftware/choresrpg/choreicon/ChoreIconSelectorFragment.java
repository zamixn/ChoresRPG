package com.zsoftware.choresrpg.choreicon;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.zsoftware.choresrpg.R;
import com.zsoftware.choresrpg.databinding.FragmentChoreIconSelectorBinding;

public class ChoreIconSelectorFragment extends Fragment {

    private FragmentChoreIconSelectorBinding binding;
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

        binding.icon.setOnClickListener(view1 -> ShowSelectIconDialog());
        super.onViewCreated(view, savedInstanceState);
    }

    private void ShowSelectIconDialog()
    {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.chore_icon_selector_dialog);
        dialog.show();
    }
}