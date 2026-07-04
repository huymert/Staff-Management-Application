package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/*
 * MainActivity:
 * - Dùng để: làm màn hình đầu vào và điều hướng theo vai trò.
 * - Kiểm tra/xử lý: kiểm tra user đã đăng nhập chưa rồi lấy role từ Firestore.
 * - Sau đó: chuyển sang LoginActivity hoặc dashboard phù hợp.
 */
public class MainActivity extends BaseActivity {

    /*
     * onCreate:
     * - Dùng để: mở app và kiểm tra trạng thái người dùng hiện tại.
     * - Kiểm tra/xử lý: nếu chưa đăng nhập thì đưa về màn hình login, còn nếu có thì cập nhật token.
     * - Sau đó: gọi tiếp hàm lấy role để chuyển đúng màn hình.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            refreshFcmToken(currentUser.getUid());
            checkUserRole(currentUser.getUid());
        }
    }

    /*
     * refreshFcmToken:
     * - Dùng để: lấy token FCM mới nhất cho user.
     * - Kiểm tra/xử lý: gọi Firebase Messaging rồi lưu token vào Firestore.
     * - Sau đó: hệ thống có dữ liệu để gửi notification về máy người dùng.
     */
    private void refreshFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null) {
                FirebaseFirestore.getInstance()
                        .collection("Users").document(uid)
                        .update("fcmToken", token);
            }
        });
    }

    /*
     * checkUserRole:
     * - Dùng để: lấy role của người dùng từ Firestore.
     * - Kiểm tra/xử lý: đọc document Users theo uid và so role admin, manager hoặc mặc định.
     * - Sau đó: chuyển sang dashboard phù hợp và đóng màn hình trung gian này.
     */
    private void checkUserRole(String uid) {
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            if ("admin".equals(role)) {
                                startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                            } else if ("manager".equals(role)) {
                                startActivity(new Intent(MainActivity.this, ManagerDashboardActivity.class));
                            } else {
                                startActivity(new Intent(MainActivity.this, EmployeeDashboardActivity.class));
                            }
                        } else {
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        }
                    } else {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                    finish();
                });
    }
}
