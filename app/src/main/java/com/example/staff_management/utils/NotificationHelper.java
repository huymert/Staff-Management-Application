package com.example.staff_management.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationHelper {

    private static final String RAG_BASE_URL = com.example.staff_management.BuildConfig.RAG_BASE_URL;
    private static final MediaType JSON = MediaType.get("application/json");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /*
     * sendToUser:
     * - Dùng để: gửi notification tới một người dùng cụ thể.
     * - Kiểm tra/xử lý: lấy fcmToken từ Firestore rồi gọi API notify.
     * - Sau đó: đẩy thông báo về thiết bị của người nhận nếu token còn hợp lệ.
     */
    public static void sendToUser(String receiverId, String title, String body) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    String token = doc.getString("fcmToken");
                    if (token != null && !token.isEmpty()) {
                        sendNotification(token, title, body);
                    }
                });
    }

    private static void sendNotification(String fcmToken, String title, String body) {
        JsonObject payload = new JsonObject();
        payload.addProperty("token", fcmToken);
        payload.addProperty("title", title);
        payload.addProperty("body", body);

        Request request = new Request.Builder()
                .url(RAG_BASE_URL + "/api/notify")
                .addHeader("X-API-Key", com.example.staff_management.BuildConfig.RAG_API_KEY)
                .post(RequestBody.create(payload.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }
}
