package com.example.hotelease;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Firebase {

    private static final String TAG = "ChatGPT";
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ResponseCallback {
        void onResponse(String response);
    }

    public static void askAI(String userMessage, String roomContext, ResponseCallback callback) {
        Log.d(TAG, "Starting API call for message: " + userMessage);

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                // Comprehensive hotel-specific system prompt
                String systemPrompt =
                        "You are a helpful and friendly hotel assistant for HotelEase. " +
                                "Your job is to assist guests with reservations, room availability, food orders, " +
                                "hotel policies, amenities, and general inquiries about their stay. " +
                                "Only answer hotel-related questions. If a guest asks something unrelated to the hotel, " +
                                "politely let them know you can only assist with hotel-related topics and redirect them.\n\n" +

                                "=== HOTEL POLICIES ===\n" +
                                "- Check-in time: 2:00 PM\n" +
                                "- Check-out time: 12:00 PM (noon)\n" +
                                "- Early check-in and late check-out are subject to availability.\n" +
                                "- Pets are not allowed on the premises.\n" +
                                "- Smoking is strictly prohibited inside all rooms and common areas.\n" +
                                "- Quiet hours are from 10:00 PM to 8:00 AM.\n\n" +

                                "=== ROOM PRICING ===\n" +
                                "- All rooms are priced at $20 per night.\n" +
                                "- The hotel has 12 rooms in total.\n" +
                                "- Room types available: Deluxe Room (King Bed), Standard Room (Queen Bed), " +
                                "Suite Room (2 Twin Beds), Executive Room (King Bed).\n" +
                                "- Each room accommodates up to 2 guests.\n\n" +

                                "=== FOOD & DINING ===\n" +
                                "- Room service is available from 7:00 AM to 10:00 PM.\n" +
                                "- Breakfast is served in the dining area from 7:00 AM to 10:30 AM.\n" +
                                "- Lunch is served from 12:00 PM to 3:00 PM.\n" +
                                "- Dinner is served from 6:00 PM to 10:00 PM.\n" +
                                "- Guests can order food through the HotelEase app.\n\n" +

                                "=== AMENITIES ===\n" +
                                "- Free high-speed Wi-Fi throughout the hotel.\n" +
                                "- Outdoor swimming pool open from 8:00 AM to 9:00 PM.\n" +
                                "- Fitness center open 24 hours.\n" +
                                "- Complimentary parking available for all guests.\n" +
                                "- Concierge service available at the front desk 24/7.\n\n" +

                                "=== RESERVATIONS ===\n" +
                                "- Guests can reserve, view, and cancel their room bookings through the HotelEase app.\n" +
                                "- Each guest may only hold one active reservation at a time.\n" +
                                "- Cancellations can be made at any time through the app at no charge.\n\n" +

                                "You will be provided with the current real-time room availability data before each guest message. " +
                                "Use this data to give accurate answers about which rooms are available or reserved. " +
                                "Never reveal the email addresses or personal details of other guests. " +
                                "However, if the guest is asking about their own reservation, you may confirm their room number, " +
                                "check-in date, check-out date, and total cost based on the data provided.";

                // Combine room context with the user's message
                String fullMessage = (roomContext != null && !roomContext.isEmpty())
                        ? roomContext + "\n\nGuest message: " + userMessage
                        : userMessage;

                // Build the JSON request
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-3.5-turbo");
                requestBody.put("max_tokens", 200);
                requestBody.put("temperature", 0.7);

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.put(systemMessage);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", fullMessage);
                messages.put(userMsg);

                requestBody.put("messages", messages);

                Log.d(TAG, "Request body: " + requestBody.toString());

                // Make the API call
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    Log.d(TAG, "Full Response: " + response.toString());

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    String aiReply = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    Log.d(TAG, "AI Response: " + aiReply);

                    final String finalReply = aiReply.trim();
                    mainHandler.post(() -> callback.onResponse(finalReply));

                } else {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();

                    String errorMsg = errorResponse.toString();
                    Log.e(TAG, "Error Response (Code " + responseCode + "): " + errorMsg);

                    try {
                        JSONObject errorJson = new JSONObject(errorMsg);
                        if (errorJson.has("error")) {
                            JSONObject error = errorJson.getJSONObject("error");
                            String errorMessage = error.optString("message", "Unknown error");
                            mainHandler.post(() -> callback.onResponse("Error: " + errorMessage));
                        } else {
                            mainHandler.post(() -> callback.onResponse(
                                    "Sorry, I couldn't process your request. (Error " + responseCode + ")"));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onResponse(
                                "Sorry, I couldn't process your request. Please try again."));
                    }
                }

            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Timeout error", e);
                mainHandler.post(() -> callback.onResponse(
                        "Request timed out. Please check your internet connection."));

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Network error - cannot reach server", e);
                mainHandler.post(() -> callback.onResponse(
                        "Cannot connect to server. Please check your internet connection."));

            } catch (Exception e) {
                Log.e(TAG, "Error calling ChatGPT API", e);
                e.printStackTrace();
                mainHandler.post(() -> callback.onResponse(
                        "Sorry, something went wrong: " + e.getMessage()));

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    // Overload for backward compatibility (no room context)
    public static void askAI(String userMessage, ResponseCallback callback) {
        askAI(userMessage, null, callback);
    }
}
