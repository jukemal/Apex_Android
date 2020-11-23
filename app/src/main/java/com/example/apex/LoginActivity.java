package com.example.apex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apex.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        hideKeyboard();

        binding.buttonLogin.setOnClickListener(v -> {
            binding.buttonLogin.showLoading();

            String email = binding.textLoginEmail.getText().toString();
            String password = binding.textLoginPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                binding.buttonLogin.hideLoading();
                if (email.isEmpty()) {
                    binding.textLoginEmail.setError("Please Enter Your Email.");
                    binding.textLoginEmail.requestFocus();
                } else {
                    binding.textLoginEmail.setError(null);
                }
                if (password.isEmpty()) {
                    binding.textLoginPassword.setError("Please Enter Your Password.");
                    binding.textLoginPassword.requestFocus();
                } else {
                    binding.textLoginPassword.setError(null);
                }
            } else {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                binding.buttonLogin.hideLoading();
                                Toast.makeText(LoginActivity.this, "Login Error, Please Login Again", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
        });

        binding.nestedScroller.setOnClickListener(v -> {
            hideKeyboard();
        });

        binding.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(binding.getRoot().getApplicationWindowToken(), 0);
    }
}