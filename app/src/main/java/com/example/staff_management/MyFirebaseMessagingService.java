package com.example.staff_management;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "staff_management_channel";
    private static final String CHANNEL_NAME = "Thông báo";

    /*
     * onMessageReceived:
     * - Dùng để: nhận thông báo đẩy từ FCM.
     * - Kiểm tra/xử lý: đọc title và body từ payload notification hoặc data.
     * - Sau đó: hiện notification lên máy người dùng.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = "Staff Management";
        String body = "";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null
                    ? remoteMessage.getNotification().getTitle() : title;
            body = remoteMessage.getNotification().getBody() != null
                    ? remoteMessage.getNotification().getBody() : body;
        } else if (!remoteMessage.getData().isEmpty()) {
            title = remoteMessage.getData().getOrDefault("title", title);
            body = remoteMessage.getData().getOrDefault("body", body);
        }

        showNotification(title, body);
    }

    /*
     * onNewToken:
     * - Dùng để: nhận token FCM mới khi hệ thống refresh.
     * - Kiểm tra/xử lý: lưu token vào Firestore theo user đang đăng nhập.
     * - Sau đó: server có token mới để gửi thông báo tiếp.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        saveTokenToFirestore(token);
    }

    /*
     * saveTokenToFirestore:
     * - Dùng để: lưu token FCM của user.
     * - Kiểm tra/xử lý: lấy uid hiện tại rồi update field fcmToken.
     * - Sau đó: app vẫn nhận được notification đầy đủ.
     */
    private void saveTokenToFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(uid)
                    .update("fcmToken", token);
        }
    }

    /*
     * showNotification:
     * - Dùng để: hiện notification cục bộ trên máy.
     * - Kiểm tra/xử lý: tạo PendingIntent rồi set nội dung cho notification.
     * - Sau đó: người dùng bấm vào có thể quay về app.
     */
    private void showNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /*
     * createNotificationChannel:
     * - Dùng để: tạo kênh thông báo cho Android 8+.
     * - Kiểm tra/xử lý: chỉ tạo khi máy chạy API O trở lên.
     * - Sau đó: notification có thể hiển thị đúng trên hệ điều hành mới.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo từ hệ thống quản lý nhân sự");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
