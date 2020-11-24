package com.example.apex.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.apex.R;
import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import org.jetbrains.annotations.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DashboardViewModel extends AndroidViewModel {
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private BluetoothManager bluetoothManager;

    @Nullable
    private SimpleBluetoothDeviceInterface deviceInterface;

    private final MutableLiveData<String> messagesData = new MutableLiveData<>();
    private final MutableLiveData<ConnectionStatus> connectionStatusData = new MutableLiveData<>();
    private final MutableLiveData<String> deviceNameData = new MutableLiveData<>();
    private final MutableLiveData<String> messageData = new MutableLiveData<>();

    private final MutableLiveData<Float> humidityData = new MutableLiveData<>();
    private final MutableLiveData<Float> temperatureDate = new MutableLiveData<>();
    private final MutableLiveData<Float> heatIndexData = new MutableLiveData<>();
    private final MutableLiveData<Float> COData = new MutableLiveData<>();
    private final MutableLiveData<Float> gasData = new MutableLiveData<>();

    private StringBuilder messages = new StringBuilder();

    private String deviceName;
    private String mac;

    private boolean connectionAttemptedOrMade = false;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean setupViewModel() {

        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(getApplication().getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        String deviceName = sharedPreferences.getString("DEVICE_NAME", "");
        String mac = sharedPreferences.getString("DEVICE_MAC", "");

        if (deviceName.equals("") || mac.equals("")) {
            return false;
        }

        bluetoothManager = BluetoothManager.getInstance();

        if (bluetoothManager == null) {
            toast("Connection Failed.");
            return false;
        }

        this.deviceName = deviceName;
        this.mac = mac;

        deviceNameData.postValue(deviceName);
        connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);

        return true;
    }

    public boolean setupViewModel(String deviceName, String mac) {
        bluetoothManager = BluetoothManager.getInstance();

        if (bluetoothManager == null) {
            toast("Connection Failed.");
            return false;
        }

        this.deviceName = deviceName;
        this.mac = mac;

        deviceNameData.postValue(deviceName);
        connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        return true;
    }

    public void connect() {
        if (!connectionAttemptedOrMade) {

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
                            connectionAttemptedOrMade = false;
                            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
                        }
                    });

            compositeDisposable.add(disposable);

            connectionAttemptedOrMade = true;
            connectionStatusData.postValue(ConnectionStatus.CONNECTING);
        }
    }

    public void disconnect() {
        if (connectionAttemptedOrMade && deviceInterface != null) {
            connectionAttemptedOrMade = false;
            bluetoothManager.closeDevice(deviceInterface);
            deviceInterface = null;
            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        }
    }

    private void onConnected(SimpleBluetoothDeviceInterface deviceInterface) {
        this.deviceInterface = deviceInterface;
        if (this.deviceInterface != null) {
            connectionStatusData.postValue(ConnectionStatus.CONNECTED);
            this.deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, t -> toast("Error sending message!"));
            toast("Connected");
            messages = new StringBuilder();
            messagesData.postValue(messages.toString());
        } else {
            toast("Connection Failed.");
            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        }
    }

    private void onMessageReceived(String message) {
        messages.append(deviceName).append(": ").append(message).append('\n');
        messagesData.postValue(messages.toString());

        Timber.e("Message : %s", message);

        for (String s : message.split(";")) {
            String[] temp = s.split(":");

            switch (temp[0]) {
                case "H":
                    try {
                        humidityData.setValue(Float.parseFloat(temp[1]));
                    } catch (Exception e) {
                        humidityData.setValue(0f);
                    }
                    break;
                case "T":
                    try {
                        temperatureDate.setValue(Float.parseFloat(temp[1]));
                    } catch (Exception e) {
                        temperatureDate.setValue(0f);
                    }
                    break;
                case "HI":
                    try {
                        heatIndexData.setValue(Float.parseFloat(temp[1]));
                    } catch (Exception e) {
                        heatIndexData.setValue(0f);
                    }
                    break;
                case "C":
                    try {
                        COData.setValue(Float.parseFloat(temp[1]));
                    } catch (Exception e) {
                        COData.setValue(0f);
                    }
                    break;
                case "G":
                    try {
                        gasData.setValue(Float.parseFloat(temp[1]));
                    } catch (Exception e) {
                        gasData.setValue(0f);
                    }
                    break;
            }
        }
    }

    private void onMessageSent(String message) {
        messages.append("You").append(": ").append(message).append('\n');
        messagesData.postValue(messages.toString());
        messageData.postValue("");
    }

    public void sendMessage(String message) {
        if (deviceInterface != null && !TextUtils.isEmpty(message)) {
            deviceInterface.sendMessage(message);
        }
    }

    @Override
    protected void onCleared() {
        compositeDisposable.dispose();

        if (bluetoothManager != null) {
            bluetoothManager.close();
        }
    }

    private void toast(String message) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show();
    }

    public LiveData<String> getMessages() {
        return messagesData;
    }

    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatusData;
    }

    public LiveData<String> getDeviceName() {
        return deviceNameData;
    }

    public LiveData<String> getMessage() {
        return messageData;
    }

    public MutableLiveData<Float> getHumidityData() {
        return humidityData;
    }

    public MutableLiveData<Float> getTemperatureDate() {
        return temperatureDate;
    }

    public MutableLiveData<Float> getHeatIndexData() {
        return heatIndexData;
    }

    public MutableLiveData<Float> getCOData() {
        return COData;
    }

    public MutableLiveData<Float> getGasData() {
        return gasData;
    }

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}