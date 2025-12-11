package com.example.hotelease;

import com.google.firebase.functions.FirebaseFunctions;

public class FirebaseFunctionsUtil {

    public interface Callback {
        void onResponse(String reply);
    }

    public static void askAI(String message, Callback callback) {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("hotelBot")
                .call(message)
                .addOnSuccessListener(result -> {
                    String reply = result.getData().toString();
                    callback.onResponse(reply);
                })
                .addOnFailureListener(e -> {
                    callback.onResponse("Error: " + e.getMessage());
                });
    }
}
