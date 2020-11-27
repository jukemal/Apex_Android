package com.example.apex;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.apex.api.maps_api.MapApiServiceGenerator;
import com.example.apex.api.weather_api.WeatherApiServiceGenerator;
import com.example.apex.databinding.ActivityMainBinding;
import com.example.apex.services.AppService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private AppService mService = null;

    private boolean mBound = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppService.LocalBinder binder = (AppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            if (!checkPermissions()) {
                requestPermissions();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.apex.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        WeatherApiServiceGenerator.setupRetrofit(getString(R.string.WEATHER_API_KEY));

        MapApiServiceGenerator.setupRetrofit(getString(R.string.MAPS_API_KEY));

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_navigation, R.id.navigation_parking, R.id.navigation_dashboard, R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        locationEnabled();
        bluetoothEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, AppService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_bluetooth) {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.navigation_bluetooth);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.e("onRequestPermissionResult");

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Timber.e("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mService.requestLocationUpdates();
            } else {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

                builder.setMessage("Permission was denied, but is needed for core functionality.")
                        .setTitle("Location Permission")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("OK", (dialogInterface, i) -> dialogInterface.cancel());

                builder.show();
            }
        }
    }

    private void locationEnabled() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!gps_enabled && !network_enabled) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

            builder.setMessage("Enable GPS to Continue.")
                    .setTitle("GPS")
                    .setPositiveButton("Settings", (dialogInterface, i) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }).setNegativeButton("OK", (dialogInterface, i) -> dialogInterface.cancel());

            builder.show();
        }
    }

    private void bluetoothEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

            builder.setMessage("This device does not support Bluetooth.")
                    .setTitle("Bluetooth")
                    .setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.cancel());

            builder.show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

            builder.setMessage("Enable Bluetooth to Continue.")
                    .setTitle("Bluetooth")
                    .setPositiveButton("Settings", (dialogInterface, i) -> {
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    }).setNegativeButton("OK", (dialogInterface, i) -> dialogInterface.cancel());

            builder.show();
        }
    }

    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Timber.e("Displaying permission rationale to provide additional context.");

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

            builder.setMessage("Location permission is needed for core functionality")
                    .setTitle("Location")
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                    });

            builder.show();
        } else {
            Timber.e("Requesting permission");

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}