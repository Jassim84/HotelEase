package com.example.hotelease;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Food_Menu extends AppCompatActivity {

    // ── Firebase ──────────────────────────────────────────────────────────────
    private DatabaseReference mRoomsRef;
    private DatabaseReference mOrdersRef;
    private String currentUserEmail;
    private int userRoomNumber = -1;
    private long roomReservationCost = 0;

    // ── Cart state ────────────────────────────────────────────────────────────
    private double cartTotal = 0.0;

    // ── Footer views ──────────────────────────────────────────────────────────
    private TextView tvCartTotal;
    private TextView tvCartCount;
    private Button btnPlaceOrder;

    // ── Item data: name → price ───────────────────────────────────────────────
    private static final Map<String, Double> PRICES = new HashMap<String, Double>() {{
        put("Classic Pancakes",       6.99);
        put("Full English",           9.99);
        put("Avocado Toast",          8.99);
        put("Continental Basket",     7.49);
        put("Acai Bowl",              8.49);
        put("Caesar Salad",           9.99);
        put("HotelEase Burger",      14.99);
        put("Club Sandwich",         11.99);
        put("Tom Yum Noodles",       12.99);
        put("Fish Tacos",            13.49);
        put("Ribeye Steak",          28.99);
        put("Grilled Lobster",       34.99);
        put("Truffle Pasta",         18.99);
        put("Chicken Supreme",       19.99);
        put("Vegetarian Wellington", 16.99);
        put("Specialty Coffee",       4.99);
        put("Fresh Juice",            4.49);
        put("Herbal Tea",             3.99);
        put("Smoothie",               6.49);
        put("Sparkling Water",        2.99);
        put("Lava Cake",              7.99);
        put("Creme Brulee",           6.99);
        put("Gelato Trio",            5.99);
        put("NY Cheesecake",          6.49);
        put("Affogato",               5.49);
    }};

    // ── Per-item view holders (qty TextView + current count) ──────────────────
    private static class ItemViews {
        TextView qtyView;
        int qty = 0;
        String name;
        double price;
    }

    private final Map<String, ItemViews> items = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_menu);

        currentUserEmail = getIntent().getStringExtra("userEmail");

        // Firebase refs
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        mRoomsRef  = db.getReference("rooms");
        mOrdersRef = db.getReference("orders");

        // Footer
        tvCartTotal  = findViewById(R.id.tvCartTotal);
        tvCartCount  = findViewById(R.id.tvCartCount);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Wire up all items
        wireItem("pancakes",    "Classic Pancakes");
        wireItem("english",     "Full English");
        wireItem("avocado",     "Avocado Toast");
        wireItem("continental", "Continental Basket");
        wireItem("acai",        "Acai Bowl");
        wireItem("caesar",      "Caesar Salad");
        wireItem("burger",      "HotelEase Burger");
        wireItem("club",        "Club Sandwich");
        wireItem("tomyum",      "Tom Yum Noodles");
        wireItem("tacos",       "Fish Tacos");
        wireItem("ribeye",      "Ribeye Steak");
        wireItem("lobster",     "Grilled Lobster");
        wireItem("pasta",       "Truffle Pasta");
        wireItem("chicken",     "Chicken Supreme");
        wireItem("wellington",  "Vegetarian Wellington");
        wireItem("coffee",      "Specialty Coffee");
        wireItem("juice",       "Fresh Juice");
        wireItem("tea",         "Herbal Tea");
        wireItem("smoothie",    "Smoothie");
        wireItem("water",       "Sparkling Water");
        wireItem("lava",        "Lava Cake");
        wireItem("brulee",      "Creme Brulee");
        wireItem("gelato",      "Gelato Trio");
        wireItem("cheesecake",  "NY Cheesecake");
        wireItem("affogato",    "Affogato");

        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        // Load room + existing order
        loadUserRoom();
    }

    /**
     * Connects the minus/qty/plus views for one item and sets up click listeners.
     * @param idSuffix  the suffix used in XML ids: minus_{suffix}, qty_{suffix}, plus_{suffix}
     * @param itemName  the display name matching the PRICES map
     */
    private void wireItem(String idSuffix, String itemName) {
        int minusId = getResId("minus_" + idSuffix);
        int qtyId   = getResId("qty_"   + idSuffix);
        int plusId  = getResId("plus_"  + idSuffix);

        TextView tvMinus = findViewById(minusId);
        TextView tvQty   = findViewById(qtyId);
        TextView tvPlus  = findViewById(plusId);

        ItemViews iv = new ItemViews();
        iv.qtyView = tvQty;
        iv.name    = itemName;
        iv.price   = PRICES.containsKey(itemName) ? PRICES.get(itemName) : 0.0;
        items.put(itemName, iv);

        tvMinus.setOnClickListener(v -> changeQty(iv, -1));
        tvPlus.setOnClickListener(v  -> changeQty(iv, +1));
    }

    private int getResId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CART LOGIC
    // ═════════════════════════════════════════════════════════════════════════

    private void changeQty(ItemViews iv, int delta) {
        iv.qty = Math.max(0, iv.qty + delta);
        iv.qtyView.setText(String.valueOf(iv.qty));
        recalcCart();
    }

    private void recalcCart() {
        cartTotal = 0;
        int totalItems = 0;
        for (ItemViews iv : items.values()) {
            if (iv.qty > 0) {
                cartTotal += iv.price * iv.qty;
                totalItems += iv.qty;
            }
        }
        tvCartTotal.setText(String.format("$%.2f", cartTotal));
        tvCartCount.setText(String.valueOf(totalItems));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FIREBASE
    // ═════════════════════════════════════════════════════════════════════════

    private void loadUserRoom() {
        mRoomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    Map<String, Object> res = (Map<String, Object>) roomSnap.getValue();
                    if (res != null && currentUserEmail != null
                            && currentUserEmail.equals(res.get("email"))) {
                        userRoomNumber = Integer.parseInt(roomSnap.getKey());
                        if (res.containsKey("totalCost"))
                            roomReservationCost = ((Number) res.get("totalCost")).longValue();
                        loadExistingOrder();
                        return;
                    }
                }
                Toast.makeText(Food_Menu.this,
                        "You need a room reservation to order food.",
                        Toast.LENGTH_LONG).show();
                btnPlaceOrder.setEnabled(false);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Food_Menu.this,
                        "Error loading room data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExistingOrder() {
        if (userRoomNumber == -1) return;
        mOrdersRef.child(String.valueOf(userRoomNumber))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        for (DataSnapshot itemSnap : snapshot.getChildren()) {
                            String key = itemSnap.getKey();
                            if (key == null || key.equals("foodTotal") || key.equals("timestamp")) continue;
                            // Keys stored with underscores instead of spaces
                            String displayName = key.replace("_", " ");
                            Object val = itemSnap.getValue();
                            if (val == null) continue;
                            int qty = ((Number) val).intValue();
                            ItemViews iv = items.get(displayName);
                            if (iv != null) {
                                iv.qty = qty;
                                iv.qtyView.setText(String.valueOf(qty));
                            }
                        }
                        recalcCart();
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void placeOrder() {
        if (userRoomNumber == -1) {
            Toast.makeText(this, "No active reservation found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build order map — only items with qty > 0
        Map<String, Object> orderMap = new HashMap<>();
        boolean hasItems = false;
        for (ItemViews iv : items.values()) {
            if (iv.qty > 0) {
                String key = iv.name.replace(" ", "_");
                orderMap.put(key, iv.qty);
                hasItems = true;
            }
        }

        if (!hasItems) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        orderMap.put("foodTotal", cartTotal);
        orderMap.put("timestamp", System.currentTimeMillis());

        // Save to orders/{roomNumber}
        mOrdersRef.child(String.valueOf(userRoomNumber)).setValue(orderMap)
                .addOnSuccessListener(aVoid -> {
                    // Update the room's totalCost = reservation cost + food cost
                    long newTotal = roomReservationCost + Math.round(cartTotal);
                    mRoomsRef.child(String.valueOf(userRoomNumber))
                            .child("totalCost").setValue(newTotal)
                            .addOnSuccessListener(a ->
                                    Toast.makeText(this,
                                            "Order placed! 🎉 Total updated to $" + newTotal,
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to place order: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}