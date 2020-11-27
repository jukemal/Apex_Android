package com.example.apex.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.apex.MainActivity;
import com.example.apex.R;
import com.example.apex.utils.LocationServiceInterface;

import java.util.Random;

import timber.log.Timber;

public class AppService extends Service {

    private static final String PACKAGE_NAME = "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    private static final String TAG = AppService.class.getSimpleName();

    public static final String CHANNEL_ID = "channel_01";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    private static final int NOTIFICATION_ID = 12345678;
    private static final int ACCIDENT_NOTIFICATION_ID = 22222222;

    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    private Handler mServiceHandler;

    private BluetoothService bluetoothService;

    private LocationService locationService;

    public AppService() {
    }

    @Override
    public void onCreate() {

        locationService = new LocationService(getApplicationContext(), new LocationServiceInterface() {
            @Override
            public void stopService() {
                stopSelf();
            }

            @Override
            public void showAccidentNotification() {
                Random random = new Random();

                Notification builder = new NotificationCompat.Builder(getApplicationContext(), AppService.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Apex")
                        .setContentText("This is a high accident area. Drive Safely")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build();

                startForeground(ACCIDENT_NOTIFICATION_ID + random.nextInt(50), builder);
            }
        });

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            mNotificationManager.createNotificationChannel(mChannel);
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver, new IntentFilter(MSERVICEBROADCASTRECEIVERACTION));

        bluetoothService = new BluetoothService(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.e("Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.e("in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Timber.e("in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.e("Last client unbound from service");

        if (!mChangingConfiguration) {
            Timber.e("Starting foreground service");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
        locationService.destroy();
    }

    public class LocalBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, AppService.class);

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .addAction(R.drawable.ic_launch, "Launch Activity",
                        activityPendingIntent)
                .setContentText("Apex is listening in background.")
                .setContentTitle("Apex")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        return builder.build();
    }

    public void requestLocationUpdates() {
        locationService.requestLocationUpdates();
        Timber.e("Requesting location updates");

        startService(new Intent(getApplicationContext(), AppService.class));

        try {
            locationService.requestLocationUpdates();
        } catch (SecurityException unlikely) {
            Timber.e(unlikely, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    public void removeLocationUpdates() {
        locationService.removeLocationUpdates();
    }

    public static String MSERVICEBROADCASTRECEIVERACTION = "whatevs";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.e("Bluetooth broadcast started.");

            bluetoothService.setup(intent.getStringExtra("mac"));
        }
    };
}
