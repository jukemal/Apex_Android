// Dakshina

package com.example.apex;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apex.databinding.ActivityRegisterBinding;
import com.example.apex.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import timber.log.Timber;

public class RegisterActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private final CollectionReference collectionReferenceUser = db.collection("users");

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        hideKeyboard();

        binding.buttonRegister.setOnClickListener(v -> {
            binding.buttonRegister.showLoading();

            String email = binding.textRegisterEmailAddress.getText().toString();
            final String name = binding.txtRegisterName.getText().toString();
            final String number = binding.textRegisterTelephoneNumber.getText().toString();
            String password = binding.textRegisterPassword.getText().toString();
            String rePassword = binding.textRegisterRePassword.getText().toString();

            if (email.isEmpty() || name.isEmpty() || number.isEmpty() || password.isEmpty() || rePassword.isEmpty() || !rePassword.equals(password)) {
                binding.buttonRegister.hideLoading();

                if (email.isEmpty()) {
                    binding.textRegisterEmailAddress.setError("Enter Your Email Address.");
                } else {
                    binding.textRegisterEmailAddress.setError(null);
                }
                if (name.isEmpty()) {
                    binding.txtRegisterName.setError("Enter Your Name.");
                } else {
                    binding.txtRegisterName.setError(null);
                }
                if (number.isEmpty()) {
                    binding.textRegisterTelephoneNumber.setError("Enter Your Telephone Number.");
                } else {
                    binding.textRegisterTelephoneNumber.setError(null);
                }
                if (password.isEmpty()) {
                    binding.textRegisterPassword.setError("Enter a Valid Password.");
                } else {
                    binding.textRegisterPassword.setError(null);
                }
                if (rePassword.isEmpty()) {
                    binding.textRegisterRePassword.setError("Enter a Valid Password.");
                } else if (!rePassword.equals(password)) {
                    binding.textRegisterRePassword.setError("Passwords does not match.");
                } else {
                    binding.textRegisterRePassword.setError(null);
                }
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegisterActivity.this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                                User user = User.builder()
                                        .email(email)
                                        .name(name)
                                        .telephoneNumber(number)
                                        .build();

                                assert firebaseUser != null;
                                collectionReferenceUser
                                        .document(firebaseUser.getUid())
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Timber.d("DocumentSnapshot successfully written!");

                                            Toast.makeText(getApplicationContext(), "Registration Successfully.", Toast.LENGTH_SHORT).show();

                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            binding.buttonRegister.hideLoading();

                                            Timber.e(e, "Error writing document");

                                            Toast.makeText(getApplicationContext(), "Registration Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();

                                        });
                            } else {
                                binding.buttonRegister.hideLoading();

                                Toast.makeText(getApplicationContext(), "Registration Unsuccessful, Please Try Again", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.nestedScroller.setOnClickListener(v -> hideKeyboard());

        binding.linearLayout.setOnClickListener(v -> hideKeyboard());
    }

    private void hideKeyboard() {
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(binding.getRoot().getApplicationWindowToken(), 0);
    }
}