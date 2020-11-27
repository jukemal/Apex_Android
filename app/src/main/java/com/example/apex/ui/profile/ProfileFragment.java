// Dakshina

package com.example.apex.ui.profile;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.apex.SplashActivity;
import com.example.apex.databinding.FragmentProfileBinding;
import com.example.apex.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import timber.log.Timber;

public class ProfileFragment extends Fragment {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private final DocumentReference documentReferenceUser = db.collection("users")
            .document(firebaseAuth.getCurrentUser().getUid());

    private static final int MAKE_CALL_PERMISSION_REQUEST_CODE = 1;

    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!checkPermission()) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.CALL_PHONE}, MAKE_CALL_PERMISSION_REQUEST_CODE);
        }

        documentReferenceUser.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();

                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            binding.setUser(user);
                        } else {
                            Timber.e("No such document.");
                        }
                    } else {
                        Timber.e(task.getException(), "Task failed");

                        Toast.makeText(getContext(), "No Internet Connection. Try Again", Toast.LENGTH_SHORT).show();
                    }
                });

        binding.btnLogOut.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

            builder.setMessage("Do you want to Log Off?")
                    .setTitle("Log Off")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(getActivity(), SplashActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    }).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

            builder.show();
        });

        binding.btnSOS.setOnClickListener(v -> {
            documentReferenceUser.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();

                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);

                                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + user.getTelephoneNumber())));
                            } else {
                                Timber.e("No such document.");
                            }
                        } else {
                            Timber.e(task.getException(), "Task failed");

                            Toast.makeText(getContext(), "No Internet Connection. Try Again", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MAKE_CALL_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(requireContext(), "You can call the number by clicking on the button", Toast.LENGTH_SHORT).show();
            }
        }
    }
}