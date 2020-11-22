package com.example.apex.ui.parking;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.apex.databinding.FragmentParkingBinding;
import com.example.apex.utils.LocationInterface;
import com.example.apex.viewadapters.LocationRecyclerViewAdapter;
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
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class ParkingFragment extends Fragment {

    private FragmentParkingBinding binding;

    private GoogleMap map;
    private CameraPosition cameraPosition;

    private PlacesClient placesClient;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(6.9271, 79.8612);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    private Location lastKnownLocation;

    private Place currentPlace;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;

    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private final DocumentReference documentReferenceCurrentUser = db.collection("users")
            .document(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid());

    private final CollectionReference collectionReferenceLocation = documentReferenceCurrentUser
            .collection("saved_locations");

    private LocationRecyclerViewAdapter locationRecyclerViewAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentParkingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        Places.initialize(requireContext(), getString(R.string.MAPS_API_KEY));
        placesClient = Places.createClient(requireContext());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_parking);

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
                            (FrameLayout) requireActivity().findViewById(R.id.map_parking), false);

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

        binding.bottomSheet.recyclerviewParking.setHasFixedSize(true);
        binding.bottomSheet.recyclerviewParking.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        locationRecyclerViewAdapter = new LocationRecyclerViewAdapter(requireContext(), new LocationInterface() {
            @Override
            public void setPath(LatLng latLng) {
                requestDirection(latLng);
            }

            @Override
            public void hideBottomSheet() {
                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.getRoot());
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        binding.bottomSheet.recyclerviewParking.setAdapter(locationRecyclerViewAdapter);

        getSavedLocations();

        binding.btnCurrentLocation.setOnClickListener(v -> getDeviceLocation());

        binding.btnLocationSave.setOnClickListener(v -> {
            if (currentPlace != null) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

                builder.setMessage("Do you want to save current location?")
                        .setTitle("Save Location")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            com.example.apex.models.Location location = com.example.apex.models.Location.builder()
                                    .placeId(currentPlace.getId())
                                    .name(currentPlace.getName())
                                    .latitude(currentPlace.getLatLng().latitude)
                                    .longitude(currentPlace.getLatLng().longitude)
                                    .build();

                            collectionReferenceLocation.add(location)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(requireContext(), "Successfully Saved.", Toast.LENGTH_SHORT).show();
                                        currentPlace = null;
                                        getSavedLocations();
                                    }).addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Error Saving Location. Try Again", Toast.LENGTH_SHORT).show();
                            });
                        }).setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel());

                builder.show();

            } else {
                Toast.makeText(requireContext(), "Select a Place to Save.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.txtSearch.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("LK")
                    .build(requireContext());
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });
    }

    private void getSavedLocations() {
        collectionReferenceLocation.orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<com.example.apex.models.Location> locationList = new ArrayList<>();

                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            com.example.apex.models.Location location = documentSnapshot.toObject(com.example.apex.models.Location.class);

                            locationList.add(location);
                        }

                        locationRecyclerViewAdapter.setLocationList(locationList);
                    } else {
                        Toast.makeText(requireContext(), "Error getting Locations.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Timber.e("Place: " + place.getName() + ", " + place.getId() + ", " + place.getLatLng());
                currentPlace = place;
                binding.txtSearch.setText(place.getName());
                moveCameraToLocation(place.getLatLng());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Timber.e(status.getStatusMessage());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void moveCameraToLocation(LatLng location) {
        map.clear();

        map.addMarker(new MarkerOptions().position(location));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location,
                DEFAULT_ZOOM));
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
                                currentPlace = null;
                                Route route = direction.getRouteList().get(0);
                                map.addMarker(new MarkerOptions().position(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
                                map.addMarker(new MarkerOptions().position(to));
                                ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
                                map.addPolyline(DirectionConverter.createPolyline(requireContext(), directionPositionList, 5, Color.RED));
                                setCameraWithCoordinationBounds(route);
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
                binding.txtSearch.setText("Location");
                currentPlace = null;
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
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}