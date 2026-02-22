package com.example.hotelease;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class room_reservation extends AppCompatActivity {

    private static final String TAG = "ReserveActivity";
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRoomsRef;

    private LinearLayout roomsContainer;
    private TextView tvCost;
    private Button goToMenuButton;
    private Button goToFoodMenuButton;
    private FloatingActionButton btnSupport;
    private String currentUserEmail;
    private int userReservedRoom = -1;

    private static final int TOTAL_ROOMS = 12;
    private Map<Integer, RoomCardViews> roomCardViewsMap = new HashMap<>();

    // Cached room snapshot for chat context
    private DataSnapshot latestRoomSnapshot = null;

    // Helper class to hold room card views
    private static class RoomCardViews {
        TextView roomNumber;
        TextView roomStatus;
        Button bookButton;
        MaterialCardView cardView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve);

        currentUserEmail = getIntent().getStringExtra("userEmail");

        // Initialize Firebase
        try {
            mDatabase = FirebaseDatabase.getInstance();
            mRoomsRef = mDatabase.getReference("rooms");
            mDatabase.setPersistenceEnabled(true);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage());
            Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show();
        }

        // Initialize views
        roomsContainer = findViewById(R.id.roomsContainer);
        tvCost = findViewById(R.id.tvCost);
        goToMenuButton = findViewById(R.id.btnGoToMenu);
        goToFoodMenuButton = findViewById(R.id.btnGoToFoodMenu);
        btnSupport = findViewById(R.id.btnSupport);

        goToMenuButton.setVisibility(View.GONE);

        // Create room cards dynamically
        createRoomCards();

        // Setup buttons
        setupButtons();

        // Listen for room changes
        setupFirebaseListener();
    }

    private void createRoomCards() {
        String[] roomTypes = {
                "Deluxe Room", "Standard Room", "Suite Room", "Executive Room",
                "Deluxe Room", "Standard Room", "Suite Room", "Executive Room",
                "Deluxe Room", "Standard Room", "Suite Room", "Executive Room"
        };

        String[] bedTypes = {
                "King Bed", "Queen Bed", "2 Twin Beds", "King Bed",
                "Queen Bed", "2 Twin Beds", "King Bed", "Queen Bed",
                "2 Twin Beds", "King Bed", "Queen Bed", "2 Twin Beds"
        };

        for (int i = 0; i < TOTAL_ROOMS; i++) {
            final int roomNumber = i + 1;

            // Create room card
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, dpToPx(12));
            card.setLayoutParams(cardParams);
            card.setRadius(dpToPx(16));
            card.setCardElevation(dpToPx(4));
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

            // Main horizontal layout
            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.HORIZONTAL);
            mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Room number circle
            LinearLayout numberCircle = new LinearLayout(this);
            LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                    dpToPx(70), dpToPx(70)
            );
            numberCircle.setLayoutParams(numberParams);
            numberCircle.setGravity(android.view.Gravity.CENTER);
            numberCircle.setBackgroundResource(R.drawable.room_number_bg);

            TextView tvRoomNumber = new TextView(this);
            tvRoomNumber.setText(String.valueOf(roomNumber));
            tvRoomNumber.setTextSize(28);
            tvRoomNumber.setTextColor(getResources().getColor(android.R.color.white));
            tvRoomNumber.setTypeface(null, android.graphics.Typeface.BOLD);
            numberCircle.addView(tvRoomNumber);

            // Room details layout
            LinearLayout detailsLayout = new LinearLayout(this);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            detailsParams.setMargins(dpToPx(16), 0, 0, 0);
            detailsLayout.setLayoutParams(detailsParams);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);

            TextView tvRoomType = new TextView(this);
            tvRoomType.setText(roomTypes[i]);
            tvRoomType.setTextSize(18);
            tvRoomType.setTextColor(getResources().getColor(R.color.brandDark));
            tvRoomType.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            infoParams.setMargins(0, dpToPx(4), 0, 0);
            infoLayout.setLayoutParams(infoParams);

            TextView tvGuests = new TextView(this);
            tvGuests.setText("👤 2 Guests  ");
            tvGuests.setTextSize(13);
            tvGuests.setTextColor(getResources().getColor(android.R.color.darker_gray));

            TextView tvBed = new TextView(this);
            tvBed.setText("🛏️ " + bedTypes[i]);
            tvBed.setTextSize(13);
            tvBed.setTextColor(getResources().getColor(android.R.color.darker_gray));

            infoLayout.addView(tvGuests);
            infoLayout.addView(tvBed);

            TextView tvStatus = new TextView(this);
            tvStatus.setText("Available");
            tvStatus.setTextSize(12);
            tvStatus.setTextColor(getResources().getColor(R.color.brandGreen));
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.setMargins(0, dpToPx(4), 0, 0);
            tvStatus.setLayoutParams(statusParams);

            detailsLayout.addView(tvRoomType);
            detailsLayout.addView(infoLayout);
            detailsLayout.addView(tvStatus);

            // Book button
            Button btnBook = new Button(this);
            btnBook.setText("Book");
            btnBook.setTextSize(14);
            btnBook.setAllCaps(false);
            btnBook.setBackgroundColor(getResources().getColor(R.color.brandGreen));
            btnBook.setTextColor(getResources().getColor(android.R.color.white));
            btnBook.setPadding(dpToPx(20), 0, dpToPx(20), 0);
            btnBook.setOnClickListener(v -> handleRoomClick(roomNumber));

            // Add views to main layout
            mainLayout.addView(numberCircle);
            mainLayout.addView(detailsLayout);
            mainLayout.addView(btnBook);

            card.addView(mainLayout);
            roomsContainer.addView(card);

            // Store references
            RoomCardViews views = new RoomCardViews();
            views.roomNumber = tvRoomNumber;
            views.roomStatus = tvStatus;
            views.bookButton = btnBook;
            views.cardView = card;
            roomCardViewsMap.put(roomNumber, views);
        }
    }

    private void setupButtons() {
        goToFoodMenuButton.setOnClickListener(v -> {
            if (userReservedRoom == -1) {
                Toast.makeText(this, "You need a reservation to order food.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(room_reservation.this, Food_Menu.class);
            intent.putExtra("userEmail", currentUserEmail);
            startActivity(intent);
        });  // <-- was missing this closing parenthesis

        goToMenuButton.setOnClickListener(v -> {
            showReservationDetails();
        });

        btnSupport.setOnClickListener(v -> showChatPopup());
    }

    private void setupFirebaseListener() {
        mRoomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Data changed in Firebase");

                // Cache the latest snapshot for chat context
                latestRoomSnapshot = snapshot;

                boolean foundUserReservation = false;

                for (int roomNumber = 1; roomNumber <= TOTAL_ROOMS; roomNumber++) {
                    RoomCardViews views = roomCardViewsMap.get(roomNumber);

                    if (snapshot.child(String.valueOf(roomNumber)).exists()) {
                        Map<String, Object> reservation =
                                (Map<String, Object>) snapshot.child(String.valueOf(roomNumber)).getValue();

                        if (reservation != null && reservation.containsKey("email")) {
                            String email = reservation.get("email").toString();

                            views.cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                            views.roomNumber.setBackgroundResource(R.drawable.room_number_bg_red);
                            views.roomStatus.setText("Reserved");
                            views.roomStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                            if (email.equals(currentUserEmail)) {
                                userReservedRoom = roomNumber;
                                goToMenuButton.setVisibility(View.VISIBLE);
                                foundUserReservation = true;
                                views.bookButton.setText("Cancel");
                                views.bookButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                if (reservation.containsKey("totalCost")) {
                                    long cost = ((Number) reservation.get("totalCost")).longValue();
                                    tvCost.setText("Total Cost: $" + cost);
                                }
                            } else {
                                views.bookButton.setEnabled(false);
                                views.bookButton.setText("Booked");
                                views.bookButton.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                            }
                        }
                    } else {
                        views.roomNumber.setBackgroundResource(R.drawable.room_number_bg);
                        views.roomStatus.setText("Available");
                        views.roomStatus.setTextColor(getResources().getColor(R.color.brandGreen));
                        views.bookButton.setText("Book");
                        views.bookButton.setEnabled(true);
                        views.bookButton.setBackgroundColor(getResources().getColor(R.color.brandGreen));
                    }
                }

                if (!foundUserReservation) {
                    userReservedRoom = -1;
                    tvCost.setText("Total Cost: $0");
                    goToMenuButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(room_reservation.this,
                        "Error loading rooms: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Builds a plain-text summary of all rooms from the cached Firebase snapshot.
     * Email addresses are intentionally omitted to protect guest privacy.
     */
    private String buildRoomContext() {
        if (latestRoomSnapshot == null) {
            return "Room data is not available yet.";
        }

        StringBuilder context = new StringBuilder("Current hotel room status (12 rooms total):\n");

        for (int i = 1; i <= TOTAL_ROOMS; i++) {
            if (latestRoomSnapshot.child(String.valueOf(i)).exists()) {
                Map<String, Object> reservation =
                        (Map<String, Object>) latestRoomSnapshot.child(String.valueOf(i)).getValue();

                if (reservation != null) {
                    String startDate = reservation.containsKey("startDate")
                            ? reservation.get("startDate").toString() : "unknown";
                    String endDate = reservation.containsKey("endDate")
                            ? reservation.get("endDate").toString() : "unknown";
                    long cost = reservation.containsKey("totalCost")
                            ? ((Number) reservation.get("totalCost")).longValue() : 0;

                    // Note: is it the current user's room?
                    boolean isYours = (i == userReservedRoom);
                    context.append("Room ").append(i).append(": Reserved")
                            .append(isYours ? " (your reservation)" : "")
                            .append(" | Check-in: ").append(startDate)
                            .append(" | Check-out: ").append(endDate)
                            .append(" | Cost: $").append(cost)
                            .append("\n");
                }
            } else {
                context.append("Room ").append(i).append(": Available\n");
            }
        }

        return context.toString();
    }

    private void showChatPopup() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_popup, null);
        dialog.setContentView(view);

        TextView botReply = view.findViewById(R.id.botReply);
        TextView userMsg = view.findViewById(R.id.userMessage);
        Button sendBtn = view.findViewById(R.id.btnSend);

        sendBtn.setOnClickListener(v -> {
            String msg = userMsg.getText().toString().trim();

            if (msg.isEmpty()) {
                botReply.setText("Please type a message.");
                return;
            }

            botReply.setText("Thinking...");

            // Build room context from cached snapshot and pass it to the AI
            String roomContext = buildRoomContext();
            Firebase.askAI(msg, roomContext, reply -> botReply.setText(reply));
        });

        dialog.show();
    }

    private void handleRoomClick(int roomNumber) {
        if (userReservedRoom == -1) {
            openDatePicker(roomNumber);
        } else if (roomNumber == userReservedRoom) {
            showUnreserveDialog(roomNumber);
        } else {
            Toast.makeText(this, "You already reserved Room " + userReservedRoom + "!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showUnreserveDialog(int roomNumber) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Do you want to cancel your reservation for Room " + roomNumber + "?")
                .setPositiveButton("Yes", (dialog, which) -> unreserveRoom(roomNumber))
                .setNegativeButton("No", null)
                .show();
    }

    private void openDatePicker(int roomNumber) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog startDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day);

            DatePickerDialog endDialog = new DatePickerDialog(this, (view1, year2, month2, day2) -> {
                Calendar end = Calendar.getInstance();
                end.set(year2, month2, day2);

                if (end.before(start)) {
                    Toast.makeText(this, "End date must be after start date!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                long days = (end.getTimeInMillis() - start.getTimeInMillis())
                        / (1000 * 60 * 60 * 24) + 1;
                long totalCost = days * 20;

                showReservationConfirmation(roomNumber, start, end, days, totalCost);

            }, year, month, day);

            endDialog.getDatePicker().setMinDate(start.getTimeInMillis());
            endDialog.setTitle("Select End Date");
            endDialog.show();

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        startDialog.getDatePicker().setMinDate(c.getTimeInMillis());
        startDialog.setTitle("Select Start Date");
        startDialog.show();
    }

    private void showReservationConfirmation(int roomNumber, Calendar start, Calendar end,
                                             long days, long totalCost) {
        String startDate = formatDate(start);
        String endDate = formatDate(end);

        String message = "Room: " + roomNumber + "\n" +
                "Check-in: " + startDate + "\n" +
                "Check-out: " + endDate + "\n" +
                "Duration: " + days + " night(s)\n" +
                "Total Cost: $" + totalCost;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Reservation")
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> reserveRoom(roomNumber, start, end, totalCost))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
    }

    private void reserveRoom(int roomNumber, Calendar start, Calendar end, long cost) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", currentUserEmail);
        data.put("startDate", formatDate(start));
        data.put("endDate", formatDate(end));
        data.put("totalCost", cost);
        data.put("timestamp", System.currentTimeMillis());

        mRoomsRef.child(String.valueOf(roomNumber)).setValue(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Room " + roomNumber + " reserved successfully");
                    Toast.makeText(this, "Room " + roomNumber + " reserved successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reserve room: " + e.getMessage());
                    Toast.makeText(this, "Failed to reserve room: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void unreserveRoom(int roomNumber) {
        mRoomsRef.child(String.valueOf(roomNumber)).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Room " + roomNumber + " unreserved successfully");
                    Toast.makeText(this, "Room " + roomNumber + " reservation cancelled!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to unreserve room: " + e.getMessage());
                    Toast.makeText(this, "Failed to cancel reservation: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showReservationDetails() {
        if (userReservedRoom == -1) {
            Toast.makeText(this, "You have no active reservation",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mRoomsRef.child(String.valueOf(userReservedRoom)).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> reservation = (Map<String, Object>) snapshot.getValue();

                        String startDate = reservation.get("startDate").toString();
                        String endDate = reservation.get("endDate").toString();
                        long cost = ((Number) reservation.get("totalCost")).longValue();

                        String details = "Your Reservation:\n\n" +
                                "Room Number: " + userReservedRoom + "\n" +
                                "Check-in: " + startDate + "\n" +
                                "Check-out: " + endDate + "\n" +
                                "Total Cost: $" + cost;

                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Reservation Details")
                                .setMessage(details)
                                .setPositiveButton("OK", null)
                                .setNegativeButton("Cancel Reservation", (dialog, which) -> {
                                    showUnreserveDialog(userReservedRoom);
                                })
                                .show();
                    }
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
