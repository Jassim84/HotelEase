package com.example.hotelease;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class reserve extends AppCompatActivity {

    private FirebaseDatabase mDatabase;
    private DatabaseReference mRoomsRef;

    private GridLayout gridLayout;
    private String currentUserEmail;
    private int userReservedRoom = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve);

        currentUserEmail = getIntent().getStringExtra("userEmail");

        mDatabase = FirebaseDatabase.getInstance();
        mRoomsRef = mDatabase.getReference("rooms");

        gridLayout = findViewById(R.id.gridLayout);
        TextView tvCost = findViewById(R.id.tvCost);

        Button goToMenuButton = findViewById(R.id.btnGoToMenu);
        goToMenuButton.setVisibility(View.GONE);

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            Button roomButton = (Button) gridLayout.getChildAt(i);
            final int roomNumber = i + 1;
            roomButton.setOnClickListener(v -> handleRoomClick(roomNumber, goToMenuButton));
        }

        mRoomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    Button roomButton = (Button) gridLayout.getChildAt(i);
                    int roomNumber = i + 1;

                    if (snapshot.child(String.valueOf(roomNumber)).exists()) {
                        Map<String, Object> reservation =
                                (Map<String, Object>) snapshot.child(String.valueOf(roomNumber)).getValue();

                        if (reservation != null && reservation.containsKey("email")) {
                            String email = reservation.get("email").toString();

                            roomButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                            roomButton.setText("Room " + roomNumber + " - Reserved");

                            if (email.equals(currentUserEmail)) {
                                userReservedRoom = roomNumber;
                                goToMenuButton.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        roomButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                        roomButton.setText("Room " + roomNumber);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        setupSupportButton();
    }

    private void setupSupportButton() {
        findViewById(R.id.btnSupport).setOnClickListener(v -> showChatPopup());
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

            FirebaseFunctionsUtil.askAI(msg, reply -> botReply.setText(reply));
        });

        dialog.show();
    }

    private void handleRoomClick(int roomNumber, Button goToMenuButton) {
        if (userReservedRoom == -1) {
            openDatePicker(roomNumber);
        } else if (roomNumber == userReservedRoom) {
            unreserveRoom(roomNumber);
            goToMenuButton.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "You already reserved a room!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDatePicker(int roomNumber) {
        Calendar c = Calendar.getInstance();

        DatePickerDialog startDialog = new DatePickerDialog(this, (view, y, m, d) -> {

            Calendar start = Calendar.getInstance();
            start.set(y, m, d);

            DatePickerDialog endDialog = new DatePickerDialog(this, (view1, y2, m2, d2) -> {

                Calendar end = Calendar.getInstance();
                end.set(y2, m2, d2);

                long days = (end.getTimeInMillis() - start.getTimeInMillis())
                        / (1000 * 60 * 60 * 24) + 1;

                long totalCost = days * 20;

                reserveRoom(roomNumber, start, end, totalCost);

            }, y, m, d);

            endDialog.show();

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        startDialog.show();
    }

    private void reserveRoom(int roomNumber, Calendar start, Calendar end, long cost) {
        Map<String, Object> data = new HashMap<>();

        data.put("email", currentUserEmail);
        data.put("startDate", start.get(Calendar.YEAR) + "-" + (start.get(Calendar.MONTH)+1) + "-" + start.get(Calendar.DAY_OF_MONTH));
        data.put("endDate", end.get(Calendar.YEAR) + "-" + (end.get(Calendar.MONTH)+1) + "-" + end.get(Calendar.DAY_OF_MONTH));
        data.put("totalCost", cost);

        mRoomsRef.child(String.valueOf(roomNumber)).setValue(data);

        Toast.makeText(this, "Room reserved!", Toast.LENGTH_SHORT).show();
    }

    private void unreserveRoom(int roomNumber) {
        mRoomsRef.child(String.valueOf(roomNumber)).removeValue();
        Toast.makeText(this, "Room unreserved!", Toast.LENGTH_SHORT).show();
    }
}
