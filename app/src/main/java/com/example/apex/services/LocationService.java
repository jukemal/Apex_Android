// Yasiru

package com.example.apex.services;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.apex.models.Accident;
import com.example.apex.utils.LocationServiceInterface;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.SphericalUtil;

import timber.log.Timber;

public class LocationService {

    private static final String PACKAGE_NAME = "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    public static final String ACCIDENT_BROADCAST = PACKAGE_NAME + ".accident";

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final double RADIOS = 200;

    private LocationRequest mLocationRequest;

    private final FusedLocationProviderClient mFusedLocationClient;

    private final LocationCallback mLocationCallback;

    private Location mLocation;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference collectionReferenceAccident = db.collection("accidents");

    private final Context context;
    private final LocationServiceInterface locationServiceInterface;

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

        LocalBroadcastManager.getInstance(context).registerReceiver(accidentBroadcastReceiver, new IntentFilter(ACCIDENT_BROADCAST));

    }

    private void onNewLocation(Location location) {
        Timber.e("New location: " + location);

        mLocation = location;

        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        checkAccidentStatus(location);
    }

    private void checkAccidentStatus(Location location) {
        collectionReferenceAccident.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            Accident accident = documentSnapshot.toObject(Accident.class);

                            double distance = SphericalUtil.computeDistanceBetween(new LatLng(accident.getLatitude(), accident.getLongitude()), new LatLng(location.getLatitude(), location.getLongitude()));

                            if (distance < RADIOS) {
                                locationServiceInterface.showAccidentNotification();
                                break;
                            }
                        }
                    } else {
                        Timber.e("Error fetching locations.");
                    }
                });
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

    public void destroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(accidentBroadcastReceiver);
    }

    private final BroadcastReceiver accidentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.e("Accident Happened");

            updateAccidentCount();
        }
    };

    private void updateAccidentCount() {
        collectionReferenceAccident.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 0) {
                            Accident accident = new Accident(mLocation.getLatitude(), mLocation.getLongitude(), 0);

                            collectionReferenceAccident.add(accident)
                                    .addOnSuccessListener(documentReference -> Timber.e("Successfully Added")).addOnFailureListener(e -> Timber.e("Error adding document"));
                        } else {
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                Accident accident = documentSnapshot.toObject(Accident.class);

                                double distance = SphericalUtil.computeDistanceBetween(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

                                if (distance < RADIOS) {
                                    documentSnapshot.getReference().update("count", accident.getCount() + 1)
                                            .addOnSuccessListener(aVoid -> Timber.e("Successfully Updated")).addOnFailureListener(e -> Timber.e("Error adding document"));

                                    break;
                                }
                            }
                        }
                    } else {
                        Timber.e("Error fetching locations.");
                    }
                });
    }
}
