package com.example.apex.viewadapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apex.R;
import com.example.apex.databinding.LayoutBluetoothRecyclerviewItemBinding;

import java.util.List;

public class BluetoothRecyclerViewAdapter extends RecyclerView.Adapter<BluetoothRecyclerViewAdapter.BluetoothRecyclerViewHolder> {

    private final Context context;
    private final NavController navController;
    private List<BluetoothDevice> bluetoothDeviceList;

    public BluetoothRecyclerViewAdapter(Context context, NavController navController) {
        this.context = context;
        this.navController = navController;
    }

    @NonNull
    @Override
    public BluetoothRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutBluetoothRecyclerviewItemBinding binding = LayoutBluetoothRecyclerviewItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new BluetoothRecyclerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothRecyclerViewHolder holder, int position) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceList.get(position);
        holder.bind(bluetoothDevice, position);
    }

    @Override
    public int getItemCount() {
        return bluetoothDeviceList == null ? 0 : bluetoothDeviceList.size();
    }

    public void setBluetoothDeviceList(List<BluetoothDevice> bluetoothDeviceList) {
        this.bluetoothDeviceList = bluetoothDeviceList;
        notifyDataSetChanged();
    }

    public class BluetoothRecyclerViewHolder extends RecyclerView.ViewHolder {

        private final LayoutBluetoothRecyclerviewItemBinding binding;

        public BluetoothRecyclerViewHolder(@NonNull LayoutBluetoothRecyclerviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(BluetoothDevice bluetoothDevice, int position) {
            binding.txtPosition.setText((position + 1) + ". ");
            binding.txtDeviceName.setText(bluetoothDevice.getName());
            binding.txtDeviceMac.setText(bluetoothDevice.getAddress());

            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("DEVICE_NAME", bluetoothDevice.getName());
                bundle.putString("DEVICE_MAC", bluetoothDevice.getAddress());
                navController.popBackStack(R.id.navigation_bluetooth, true);
                navController.navigate(R.id.navigation_dashboard, bundle);
            });
        }
    }
}
