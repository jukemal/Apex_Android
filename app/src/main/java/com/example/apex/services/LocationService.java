package com.example.apex.services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.apex.utils.LocationServiceInterface;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import timber.log.Timber;

public class LocationService {

    private static final String PACKAGE_NAME = "com.google.android.gms.location.sample.locationupdatesforegroundservice";
    
    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;

    private Location mLocation;

    private Context context;
    private LocationServiceInterface locationServiceInterface;

    public LocationService(Context context, LocationServiceInterface locationServiceInterface) {
        this.context = context;
        this.locationServiceInterface = locationServiceInterface;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();
    }

    private void onNewLocation(Location location) {
        Timber.e("New location: " + location);

        mLocation = location;

        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        locationServiceInterface.updateNotification();
    }

    public void requestLocationUpdates() {
        if (!((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED))) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    public void removeLocationUpdates() {
        Timber.e("Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            locationServiceInterface.stopService();
        } catch (SecurityException unlikely) {
            Timber.e(unlikely, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLocation = task.getResult();
                        } else {
                            Timber.e("Failed to get location.");
                        }
                    });
        } catch (SecurityException unlikely) {
            Timber.e("Lost location permission." + unlikely);
        }
    }
}
