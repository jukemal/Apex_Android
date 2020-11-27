// Dakshina

package com.example.apex.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.apex.R;
import com.example.apex.ui.dashboard.DashboardFragment;
import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class BluetoothService {

    private static final String PACKAGE_NAME = "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    public static final String BLUETOOTHBROADCAST = PACKAGE_NAME + ".broadcast.bluetooth";

    Context context;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BluetoothService(Context context) {
        this.context = context;

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        String mac = sharedPreferences.getString("DEVICE_MAC", "");

        if (!mac.equals("")) {
            setup(mac);
        }
    }

    public void setup(String mac) {
        Timber.e("Bluetooth setup starting for MAC " + mac);

        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.isDisposed();
        }

        BluetoothManager bluetoothManager = BluetoothManager.getInstance();

        if (bluetoothManager == null) {
            toast("Connection Failed.");
        } else {
            Disposable disposable = bluetoothManager.openSerialDevice(mac)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<BluetoothSerialDevice>() {
                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull BluetoothSerialDevice bluetoothSerialDevice) {
                            onConnected(bluetoothSerialDevice.toSimpleDeviceInterface());
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            toast("Connection Failed.");
                        }
                    });

            compositeDisposable.add(disposable);
        }
    }

    private void onConnected(SimpleBluetoothDeviceInterface deviceInterface) {
        if (deviceInterface != null) {
            deviceInterface.setListeners(this::onMessageReceived, m -> Timber.e(m), t -> toast("Error sending message!"));
            toast("Bluetooth Connected");
        } else {
            toast("Bluetooth Connection Failed.");
        }
    }

    private void onMessageReceived(String message) {
        Timber.e("Bluetooth Message : " + message);

        Intent intent = new Intent(BLUETOOTHBROADCAST);
        intent.putExtra(DashboardFragment.Dashboard_Data, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        String[] temp = message.split(":");

        switch (temp[0]) {
            case "A":
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                Intent i = new Intent(LocationService.ACCIDENT_BROADCAST);
                localBroadcastManager.sendBroadcast(i);
                break;
        }
    }


    private void toast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
