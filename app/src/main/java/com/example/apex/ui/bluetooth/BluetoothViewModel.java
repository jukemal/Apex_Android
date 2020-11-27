// Dakshina

package com.example.apex.ui.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.harrysoft.androidbluetoothserial.BluetoothManager;

import java.util.List;

public class BluetoothViewModel extends AndroidViewModel {

    private BluetoothManager bluetoothManager;

    private final MutableLiveData<List<BluetoothDevice>> pairedDeviceList = new MutableLiveData<>();

    public BluetoothViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean setupViewModel() {

        bluetoothManager = BluetoothManager.getInstance();

        if (bluetoothManager == null) {
            Toast.makeText(getApplication(), "Bluetooth not available.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public void refreshPairedDevices() {
        pairedDeviceList.postValue(bluetoothManager.getPairedDevicesList());
    }

    @Override
    protected void onCleared() {
        if (bluetoothManager != null) {
            bluetoothManager.close();
        }
    }

    public LiveData<List<BluetoothDevice>> getPairedDeviceList() {
        return pairedDeviceList;
    }
}