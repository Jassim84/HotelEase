package com.example.hotelease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class registration_page extends AppCompatActivity {

    private EditText fullNameField, phoneField, emailField, passwordField, confirmPasswordField;
    private Button registerButton;
    private TextView backToLoginText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        fullNameField = findViewById(R.id.fullNameField);
        phoneField = findViewById(R.id.phoneField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerButton = findViewById(R.id.registerBtn);
        backToLoginText = findViewById(R.id.backToLoginText);

        // Back to login click listener
        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(registration_page.this, login_page.class);
            startActivity(intent);
            finish();
        });
    }

    public void register(View view) {
        String fullName = fullNameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(fullName)) {
            fullNameField.setError("Full name is required");
            fullNameField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneField.setError("Phone number is required");
            phoneField.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            phoneField.setError("Enter a valid phone number");
            phoneField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            emailField.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Enter a valid email");
            emailField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            passwordField.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            passwordField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordField.setError("Please confirm password");
            confirmPasswordField.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match");
            confirmPasswordField.requestFocus();
            return;
        }

        // Disable button to prevent multiple clicks
        registerButton.setEnabled(false);

        // Create user using Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Save additional user data to Firestore
                            saveUserToFirestore(user.getUid(), fullName, phone, email);
                        }

                    } else {
                        registerButton.setEnabled(true);
                        Toast.makeText(registration_page.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String phone, String email) {
        // Create a user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("phone", phone);
        userData.put("email", email);
        userData.put("role", "user"); // Default role
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore in "users" collection
        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(registration_page.this,
                            "Registration successful!",
                            Toast.LENGTH_SHORT).show();

                    // Go to login page
                    Intent intent = new Intent(registration_page.this, login_page.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    registerButton.setEnabled(true);
                    Toast.makeText(registration_page.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
