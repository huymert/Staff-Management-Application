package com.example.staff_management;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/*
 * AiAssistant:
 * - Dùng để: gửi câu hỏi của người dùng lên backend AI và lấy câu trả lời về.
 * - Kiểm tra/xử lý: ưu tiên xác thực bằng Firebase token, nếu lỗi thì chuyển sang demo key.
 * - Sau đó: trả kết quả qua callback để màn hình hiển thị lại cho người dùng.
 */
public class AiAssistant {

    private static final String RAG_BASE_URL = BuildConfig.RAG_BASE_URL;
    private static final String DEMO_API_KEY = BuildConfig.RAG_API_KEY;
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String sessionId;

    /*
     * AiCallback:
     * - Dùng để: nhận kết quả trả về từ AI assistant.
     * - Kiểm tra/xử lý: tách riêng luồng thành công và luồng lỗi.
     * - Sau đó: màn hình gọi class này chỉ cần xử lý đúng dữ liệu trả về.
     */
    public interface AiCallback {
        void onResponse(String response);
        void onError(Throwable t);
    }

    /*
     * AiAssistant:
     * - Dùng để: khởi tạo client gọi API và tạo session riêng cho từng cuộc trò chuyện.
     * - Kiểm tra/xử lý: cấu hình timeout và JSON parser trước khi gửi request.
     * - Sau đó: các câu hỏi sau vẫn đi chung một session để giữ ngữ cảnh.
     */
    public AiAssistant() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.sessionId = UUID.randomUUID().toString();
    }

    /*
     * ask:
     * - Dùng để: gửi câu hỏi của người dùng tới AI backend.
     * - Kiểm tra/xử lý: lấy Firebase ID token nếu có, nếu không thì dùng key demo.
     * - Sau đó: gọi tiếp request mạng để lấy câu trả lời.
     */
    public void ask(String userQuery, AiCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUser.getIdToken(false).addOnSuccessListener(result -> {
                String token = result.getToken();
                sendRequest(userQuery, "Bearer " + token, null, callback);
            }).addOnFailureListener(e ->
                    sendRequest(userQuery, null, DEMO_API_KEY, callback));
        } else {
            sendRequest(userQuery, null, DEMO_API_KEY, callback);
        }
    }

    /*
     * sendRequest:
     * - Dùng để: gọi HTTP POST lên API chat của AI.
     * - Kiểm tra/xử lý: ghép message với session_id và gắn đúng kiểu header xác thực.
     * - Sau đó: đọc JSON trả về rồi lấy answer hoặc báo lỗi cho callback.
     */
    private void sendRequest(String message, String bearerToken, String apiKey, AiCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        body.addProperty("session_id", sessionId);

        Request.Builder requestBuilder = new Request.Builder()
                .url(RAG_BASE_URL + "/api/chat")
                .post(RequestBody.create(gson.toJson(body), JSON));

        if (bearerToken != null) {
            requestBuilder.addHeader("Authorization", bearerToken);
        } else if (apiKey != null) {
            requestBuilder.addHeader("X-API-Key", apiKey);
        }

        httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError(new IOException("RAG error " + response.code() + ": " + bodyStr));
                        return;
                    }
                    JsonObject json = gson.fromJson(bodyStr, JsonObject.class);
                    if (json.has("error") && !json.get("error").isJsonNull()) {
                        String errMsg = json.getAsJsonObject("error").get("message").getAsString();
                        callback.onError(new IOException(errMsg));
                    } else {
                        String answer = json.has("answer") && !json.get("answer").isJsonNull()
                                ? json.get("answer").getAsString()
                                : "Không có phản hồi từ hệ thống.";
                        callback.onResponse(answer);
                    }
                }
            }
        });
    }
}
