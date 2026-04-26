package com.example.hotelease;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class feedback extends AppCompatActivity {

    private static final String[] RATING_LABELS = {
            "", "😞 Poor", "😕 Fair", "😊 Good", "😄 Very Good", "🤩 Excellent!"
    };

    private int selectedRating = 0;
    private ImageButton[] stars = new ImageButton[5];
    private TextView tvRatingLabel, tvCharCount, tvRoomInfoLabel;
    private TextInputEditText etFeedback;
    private Chip chipCleanliness, chipService, chipFood, chipComfort, chipLocation, chipValue;
    private Button btnSubmit;
    private LinearLayout previousFeedbackContainer;

    private String currentUserEmail;
    private int userReservedRoom = -1;

    private DatabaseReference mFeedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        currentUserEmail = getIntent().getStringExtra("userEmail");
        userReservedRoom = getIntent().getIntExtra("reservedRoom", -1);

        mFeedbackRef = FirebaseDatabase.getInstance().getReference("feedback");

        initViews();
        setupRoomInfoBanner();
        setupStarRating();
        setupCharCounter();
        setupSubmitButton();
        loadPreviousFeedback();
    }

    private void initViews() {
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvRoomInfoLabel = findViewById(R.id.tvRoomInfoLabel);
        etFeedback = findViewById(R.id.etFeedback);
        btnSubmit = findViewById(R.id.btnSubmitFeedback);
        previousFeedbackContainer = findViewById(R.id.previousFeedbackContainer);

        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        chipCleanliness = findViewById(R.id.chipCleanliness);
        chipService    = findViewById(R.id.chipService);
        chipFood       = findViewById(R.id.chipFood);
        chipComfort    = findViewById(R.id.chipComfort);
        chipLocation   = findViewById(R.id.chipLocation);
        chipValue      = findViewById(R.id.chipValue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRoomInfoBanner() {
        if (userReservedRoom != -1) {
            tvRoomInfoLabel.setText("Room " + userReservedRoom + " reservation");
        } else {
            tvRoomInfoLabel.setText("General hotel feedback");
        }
    }

    private void setupStarRating() {
        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> setRating(rating));
        }
    }

    private void setRating(int rating) {
        selectedRating = rating;
        tvRatingLabel.setText(RATING_LABELS[rating]);
        tvRatingLabel.setTextColor(getResources().getColor(R.color.brandGreen));

        for (int i = 0; i < 5; i++) {
            stars[i].setImageResource(
                    i < rating
                            ? android.R.drawable.star_big_on
                            : android.R.drawable.star_big_off
            );
        }
    }

    private void setupCharCounter() {
        etFeedback.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + " / 300");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etFeedback.getText() != null
                ? etFeedback.getText().toString().trim() : "";

        // Collect selected categories
        List<String> categories = new ArrayList<>();
        if (chipCleanliness.isChecked()) categories.add("Cleanliness");
        if (chipService.isChecked())     categories.add("Service");
        if (chipFood.isChecked())        categories.add("Dining");
        if (chipComfort.isChecked())     categories.add("Comfort");
        if (chipLocation.isChecked())    categories.add("Location");
        if (chipValue.isChecked())       categories.add("Value");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        // Firebase path: feedback/{sanitizedEmail}/{pushId}
        String safeEmail = currentUserEmail.replace(".", "_").replace("@", "_at_");

        Map<String, Object> entry = new HashMap<>();
        entry.put("email",      currentUserEmail);
        entry.put("rating",     selectedRating);
        entry.put("comment",    text);
        entry.put("categories", categories);
        entry.put("roomNumber", userReservedRoom);
        entry.put("timestamp",  timestamp);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting…");

        mFeedbackRef.child(safeEmail).push().setValue(entry)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thank you for your feedback! ⭐", Toast.LENGTH_LONG).show();
                    resetForm();
                    loadPreviousFeedback();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Feedback");
                });
    }

    private void resetForm() {
        selectedRating = 0;
        for (ImageButton star : stars) star.setImageResource(android.R.drawable.star_big_off);
        tvRatingLabel.setText("Tap a star to rate");
        tvRatingLabel.setTextColor(getResources().getColor(android.R.color.darker_gray));
        etFeedback.setText("");
        chipCleanliness.setChecked(false);
        chipService.setChecked(false);
        chipFood.setChecked(false);
        chipComfort.setChecked(false);
        chipLocation.setChecked(false);
        chipValue.setChecked(false);
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Feedback");
    }

    private void loadPreviousFeedback() {
        String safeEmail = currentUserEmail.replace(".", "_").replace("@", "_at_");

        mFeedbackRef.child(safeEmail)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        previousFeedbackContainer.removeAllViews();

                        if (!snapshot.exists()) {
                            TextView empty = new TextView(feedback.this);
                            empty.setText("No feedback submitted yet.");
                            empty.setTextColor(getResources().getColor(android.R.color.darker_gray));
                            empty.setTextSize(13);
                            previousFeedbackContainer.addView(empty);
                            return;
                        }

                        // Collect and reverse to show newest first
                        List<DataSnapshot> items = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) items.add(0, child);

                        for (DataSnapshot child : items) {
                            Map<String, Object> fb = (Map<String, Object>) child.getValue();
                            if (fb == null) continue;

                            int rating   = fb.containsKey("rating")    ? ((Number) fb.get("rating")).intValue() : 0;
                            String comment  = fb.containsKey("comment")   ? fb.get("comment").toString() : "";
                            String ts    = fb.containsKey("timestamp") ? fb.get("timestamp").toString() : "";
                            int room     = fb.containsKey("roomNumber")
                                    ? ((Number) fb.get("roomNumber")).intValue() : -1;

                            previousFeedbackContainer.addView(
                                    buildFeedbackCard(rating, comment, ts, room)
                            );
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private View buildFeedbackCard(int rating, String comment, String timestamp, int room) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(params);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Top row: stars + date
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        StringBuilder starsStr = new StringBuilder();
        for (int i = 0; i < 5; i++) starsStr.append(i < rating ? "★" : "☆");

        TextView tvStars = new TextView(this);
        tvStars.setText(starsStr.toString());
        tvStars.setTextSize(18);
        tvStars.setTextColor(0xFFFFC107); // amber
        LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tvStars.setLayoutParams(starParams);

        TextView tvDate = new TextView(this);
        tvDate.setText(timestamp);
        tvDate.setTextSize(11);
        tvDate.setTextColor(getResources().getColor(android.R.color.darker_gray));

        topRow.addView(tvStars);
        topRow.addView(tvDate);

        // Room tag
        if (room != -1) {
            TextView tvRoom = new TextView(this);
            tvRoom.setText("Room " + room);
            tvRoom.setTextSize(12);
            tvRoom.setTextColor(getResources().getColor(R.color.brandGreen));
            tvRoom.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, dpToPx(4), 0, 0);
            tvRoom.setLayoutParams(rp);
            inner.addView(topRow);
            inner.addView(tvRoom);
        } else {
            inner.addView(topRow);
        }

        // Comment
        if (!comment.isEmpty()) {
            TextView tvComment = new TextView(this);
            tvComment.setText(comment);
            tvComment.setTextSize(14);
            tvComment.setTextColor(0xFF424242);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, dpToPx(8), 0, 0);
            tvComment.setLayoutParams(cp);
            inner.addView(tvComment);
        }

        card.addView(inner);
        return card;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}