package com.example.hotelease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class login extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button manageUsersButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind UI
        emailField = findViewById(R.id.signInEmailField);
        passwordField = findViewById(R.id.signInPasswordField);
        manageUsersButton = findViewById(R.id.manageUsersButton);

        // Hide admin button first
        manageUsersButton.setVisibility(View.GONE);

        // Button click to open Manage Users
        manageUsersButton.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, manageUsers.class);
            startActivity(intent);
        });

        // Show admin button if correct admin credentials typed
        emailField.addTextChangedListener(adminWatcher);
        passwordField.addTextChangedListener(adminWatcher);
    }

    // TextWatcher for admin
    private final android.text.TextWatcher adminWatcher = new android.text.TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkAdminCredentials();
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    };

    private void checkAdminCredentials() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.equals("admin@admin.hotel") && password.equals("adminhotel")) {
            manageUsersButton.setVisibility(View.VISIBLE);
        } else {
            manageUsersButton.setVisibility(View.GONE);
        }
    }

    // Sign-in method
    public void signIn(View view) {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // ADMIN LOGIN
        if (email.equals("admin@admin.hotel") && password.equals("adminhotel")) {
            Intent intent = new Intent(login.this, manageUsers.class);
            intent.putExtra("isAdmin", true);
            startActivity(intent);
            finish();
            return;
        }

        // NORMAL USER → Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        String loggedInEmail = user != null ? user.getEmail() : email;

                        Toast.makeText(login.this, "Login successful!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(login.this, reserve.class);
                        intent.putExtra("userEmail", loggedInEmail);
                        intent.putExtra("isAdmin", false);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();


                    }
                });
    }

    public void goToRegister(View view) {
        Intent intent = new Intent(login.this, register.class);
        startActivity(intent);
    }
}
