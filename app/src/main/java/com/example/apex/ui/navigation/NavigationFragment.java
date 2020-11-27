// Ravindu

package com.example.apex.ui.navigation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.config.GoogleDirectionConfiguration;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.example.apex.R;
import com.example.apex.api.maps_api.MapApiService;
import com.example.apex.api.maps_api.MapApiServiceGenerator;
import com.example.apex.api.weather_api.WeatherApiService;
import com.example.apex.api.weather_api.WeatherApiServiceGenerator;
import com.example.apex.databinding.FragmentNavigationBinding;
import com.example.apex.models.GeoCode;
import com.example.apex.models.Weather;
import com.example.apex.models.WeatherWrapper;
import com.example.apex.viewadapters.WeatherRecyclerViewAdapter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

public class NavigationFragment extends Fragment {

    private FragmentNavigationBinding binding;

    private GoogleMap map;
    private CameraPosition cameraPosition;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(6.9271, 79.8612);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    private Location lastKnownLocation;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private CompositeDisposable compositeDisposable;

    private WeatherRecyclerViewAdapter weatherRecyclerViewAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNavigationBinding.inflate(inflater, container, false);
        compositeDisposable = new CompositeDisposable();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        hideKeyboard();

        Places.initialize(requireContext(), getString(R.string.MAPS_API_KEY));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_navigation);

        assert mapFragment != null;
        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setZoomGesturesEnabled(true);

            map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                            (FrameLayout) requireActivity().findViewById(R.id.map_navigation), false);

                    TextView title = infoWindow.findViewById(R.id.title);
                    title.setText(marker.getTitle());

                    TextView snippet = infoWindow.findViewById(R.id.snippet);
                    snippet.setText(marker.getSnippet());

                    return infoWindow;
                }
            });

            getLocationPermission();

            updateLocationUI();

            getDeviceLocation();
        });

        binding.bottomSheet.recyclerviewWeather.setHasFixedSize(true);
        binding.bottomSheet.recyclerviewWeather.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        weatherRecyclerViewAdapter = new WeatherRecyclerViewAdapter(requireContext());
        binding.bottomSheet.recyclerviewWeather.setAdapter(weatherRecyclerViewAdapter);

        binding.btnCurrentLocation.setOnClickListener(v -> getDeviceLocation());

        binding.txtTo.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("LK")
                    .build(requireContext());
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });

        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.getRoot());

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        weatherRecyclerViewAdapter.collapseCards();
                        binding.bottomSheet.arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_keyboard_arrow_up_24));
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        binding.bottomSheet.arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_keyboard_arrow_down_24));
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        hideKeyboard();
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Timber.e("Place: " + place.getName() + ", " + place.getId() + ", " + place.getLatLng());
                binding.txtTo.setText(place.getName());
                requestDirection(place.getLatLng());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Timber.e(status.getStatusMessage());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestDirection(LatLng to) {
        GoogleDirectionConfiguration.getInstance().setLogEnabled(true);

        if (lastKnownLocation != null) {
            GoogleDirection.withServerKey(getString(R.string.MAPS_API_KEY))
                    .from(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                    .to(to)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(@Nullable Direction direction) {
                            if (direction != null && direction.isOK()) {
                                map.clear();
                                Route route = direction.getRouteList().get(0);
                                map.addMarker(new MarkerOptions().position(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
                                map.addMarker(new MarkerOptions().position(to));
                                ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
                                map.addPolyline(DirectionConverter.createPolyline(requireContext(), directionPositionList, 5, Color.RED));
                                setCameraWithCoordinationBounds(route);
                                getWeatherData(directionPositionList);
                            } else {
                                Toast.makeText(requireContext(), "Error Retrieving Route Data", Toast.LENGTH_SHORT).show();
                                if (direction != null) {
                                    Timber.e(direction.getStatus());
                                }
                            }
                        }

                        @Override
                        public void onDirectionFailure(@NonNull Throwable t) {
                            Toast.makeText(requireContext(), "Error Retrieving Route Data", Toast.LENGTH_SHORT).show();
                            Timber.e(t);
                        }
                    });
        } else {
            Toast.makeText(requireContext(), "Enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private List<LatLng> selectLocations(ArrayList<LatLng> latLngArrayList) {

        if (latLngArrayList.size() <= 8) {
            return latLngArrayList;
        } else {
            List<LatLng> l = new ArrayList<>();

            l.add(latLngArrayList.get(0));
            l.add(latLngArrayList.get(latLngArrayList.size() - 1));

            int size = (latLngArrayList.size() - 2) / 3;

            List<List<LatLng>> subSets = Lists.partition(latLngArrayList, size);

            for (int i = 0; i < 3; i++) {
                l.add(subSets.get(i).get(subSets.get(i).size() / 2));
            }

            return l;
        }
    }

    private void getWeatherData(ArrayList<LatLng> latLngArrayList) {

        List<LatLng> list = selectLocations(latLngArrayList);

        Observable<LatLng> observable = Observable.fromIterable(list);

        Disposable disposable = observable
                .concatMap((Function<LatLng, ObservableSource<WeatherWrapper>>) this::getWrapper).toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<WeatherWrapper>>() {
                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull List<WeatherWrapper> weatherWrapperList) {
                        weatherRecyclerViewAdapter.setWeatherWrapperList(weatherWrapperList);
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Toast.makeText(requireContext(), "Error Getting Weather Data", Toast.LENGTH_SHORT).show();
                        Timber.e(e);
                    }
                });

        compositeDisposable.add(disposable);
    }

    private Observable<WeatherWrapper> getWrapper(LatLng latLng) {
        WeatherApiService weatherApiService = WeatherApiServiceGenerator.createService(WeatherApiService.class);

        MapApiService mapApiService = MapApiServiceGenerator.createService(MapApiService.class);

        Observable<Weather> weatherObservable = weatherApiService.getWeather(latLng.latitude, latLng.longitude);

        Observable<GeoCode> geoCodeObservable = mapApiService.getGeocode(latLng.latitude + "," + latLng.longitude);

        return Observable.zip(weatherObservable, geoCodeObservable, (weather, geoCode) -> new WeatherWrapper(latLng, weather, geoCode));
    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                map.clear();
                binding.txtTo.setText("To");
                weatherRecyclerViewAdapter.emptyWeatherWrapperList();
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            map.addMarker(new MarkerOptions().position(new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude())));
                        }
                    } else {
                        Timber.d("Current location is null. Using defaults.");
                        Timber.e("Exception: " + task.getException());
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                        map.addMarker(new MarkerOptions().position(defaultLocation));
                    }
                });
            }
        } catch (SecurityException e) {
            Timber.e(e, "Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (!locationPermissionGranted) {
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Timber.e("Exception: " + e.getMessage());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    private void hideKeyboard() {
        final InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(binding.getRoot().getApplicationWindowToken(), 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }
}