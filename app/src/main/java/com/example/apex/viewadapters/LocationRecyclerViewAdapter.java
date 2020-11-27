// Ravindu

package com.example.apex.viewadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apex.databinding.LayoutParkingRecyclerviewItemBinding;
import com.example.apex.models.Location;
import com.example.apex.utils.LocationInterface;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Objects;

public class LocationRecyclerViewAdapter extends RecyclerView.Adapter<LocationRecyclerViewAdapter.LocationRecyclerViewHolder> {

    private final Context context;
    private final LocationInterface locationInterface;
    private List<Location> locationList;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private final DocumentReference documentReferenceCurrentUser = db.collection("users")
            .document(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid());

    private final CollectionReference collectionReferenceLocation = documentReferenceCurrentUser
            .collection("saved_locations");

    public LocationRecyclerViewAdapter(Context context, LocationInterface locationInterface) {
        this.context = context;
        this.locationInterface = locationInterface;
    }

    @NonNull
    @Override
    public LocationRecyclerViewAdapter.LocationRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutParkingRecyclerviewItemBinding binding = LayoutParkingRecyclerviewItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new LocationRecyclerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationRecyclerViewAdapter.LocationRecyclerViewHolder holder, int position) {
        Location location = locationList.get(position);
        holder.bind(location, position);
    }

    @Override
    public int getItemCount() {
        return locationList == null ? 0 : locationList.size();
    }

    public void setLocationList(List<Location> locationList) {
        this.locationList = locationList;
        notifyDataSetChanged();
    }

    public class LocationRecyclerViewHolder extends RecyclerView.ViewHolder {

        private final LayoutParkingRecyclerviewItemBinding binding;

        public LocationRecyclerViewHolder(@NonNull LayoutParkingRecyclerviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Location location, int position) {
            binding.setLocation(location);
            binding.setPosition(position);
            binding.executePendingBindings();

            binding.card.setOnClickListener(v -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

                builder.setMessage("Do you want to set navigation for this location?")
                        .setTitle("Set Navigation")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            locationInterface.setPath(new LatLng(location.getLatitude(), location.getLongitude()));
                            locationInterface.hideBottomSheet();
                            Toast.makeText(context, "Navigation set successfully.", Toast.LENGTH_SHORT).show();
                        }).setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel());

                builder.show();
            });

            binding.btnDelete.setOnClickListener(v -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

                builder.setMessage("Do you want to delete this location?")
                        .setTitle("Delete Location")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            collectionReferenceLocation.document(location.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Location successfully deleted.", Toast.LENGTH_SHORT).show();
                                        locationList.remove(location);
                                        notifyDataSetChanged();
                                    }).addOnFailureListener(e -> {
                                Toast.makeText(context, "Error deleting location", Toast.LENGTH_SHORT).show();
                            });
                        }).setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel());

                builder.show();
            });
        }
    }
}
