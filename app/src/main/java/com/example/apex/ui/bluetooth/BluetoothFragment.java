// Dakshina

package com.example.apex.ui.bluetooth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.apex.databinding.FragmentBluetoothBinding;
import com.example.apex.viewadapters.BluetoothRecyclerViewAdapter;

public class BluetoothFragment extends Fragment {

    private BluetoothViewModel viewModel;

    private FragmentBluetoothBinding binding;

    private BluetoothRecyclerViewAdapter bluetoothRecyclerViewAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBluetoothBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        NavController navController = Navigation.findNavController(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);

        if (!viewModel.setupViewModel()) {
            Toast.makeText(requireContext(), "Error setting up Bluetooth", Toast.LENGTH_SHORT).show();
        }

        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        bluetoothRecyclerViewAdapter = new BluetoothRecyclerViewAdapter(requireContext(), navController);
        binding.recyclerView.setAdapter(bluetoothRecyclerViewAdapter);

        viewModel.refreshPairedDevices();

        viewModel.getPairedDeviceList().observe(getViewLifecycleOwner(), bluetoothDevices -> {
            bluetoothRecyclerViewAdapter.setBluetoothDeviceList(bluetoothDevices);
        });

        binding.mainSwiperefresh.setOnRefreshListener(() -> {
            viewModel.refreshPairedDevices();
            binding.mainSwiperefresh.setRefreshing(false);
        });
    }

}