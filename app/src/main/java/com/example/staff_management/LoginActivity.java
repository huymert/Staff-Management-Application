package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends BaseActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private ImageView ivLanguage;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /*
     * onCreate:
     * - Dùng để: khởi tạo màn hình đăng nhập cho người dùng.
     * - Kiểm tra/xử lý: lấy Firebase Auth, Firestore và ánh xạ các nút nhập liệu.
     * - Sau đó: gắn sự kiện cho đăng nhập, đổi ngôn ngữ và quên mật khẩu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivLanguage = findViewById(R.id.ivLanguage);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());
        ivLanguage.setOnClickListener(v -> showLanguageChooser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /*
     * showForgotPasswordDialog:
     * - Dùng để: mở hộp thoại nhập email lấy lại mật khẩu.
     * - Kiểm tra/xử lý: kiểm tra email có rỗng và đúng định dạng hay không.
     * - Sau đó: nếu hợp lệ thì gửi request reset mật khẩu qua Firebase.
     */
    private void showForgotPasswordDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        com.google.android.material.textfield.TextInputEditText etEmailInput = view.findViewById(R.id.etEmailInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String email = etEmailInput.getText() != null ? etEmailInput.getText().toString().trim() : "";
            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, getString(R.string.invalid_email_format), Toast.LENGTH_SHORT).show();
                return;
            }
            sendResetPasswordEmail(email);
            dialog.dismiss();
        });

        dialog.show();
    }

    /*
     * sendResetPasswordEmail:
     * - Dùng để: gửi email đặt lại mật khẩu cho người dùng.
     * - Kiểm tra/xử lý: hiện progress và chờ Firebase xử lý request.
     * - Sau đó: thông báo thành công hoặc lỗi cho người dùng.
     */
    private void sendResetPasswordEmail(String email) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.reset_link_sent), Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(this, getString(R.string.reset_link_failed, error), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /*
     * loginUser:
     * - Dùng để: xử lý đăng nhập bằng email và mật khẩu.
     * - Kiểm tra/xử lý: kiểm tra dữ liệu nhập, khóa nút đăng nhập và gọi Firebase Auth.
     * - Sau đó: nếu đúng tài khoản thì lấy role rồi chuyển màn hình theo vai trò.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        } else {
                            progressBar.setVisibility(android.view.View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(android.view.View.GONE);
                        btnLogin.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(this, getString(R.string.auth_failed) + ": " + error, Toast.LENGTH_SHORT).show();
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
        db.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            if ("admin".equals(role)) {
                                startActivity(new Intent(this, AdminDashboardActivity.class));
                            } else if ("manager".equals(role)) {
                                startActivity(new Intent(this, ManagerDashboardActivity.class));
                            } else {
                                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                            }
                            finish();
                        } else {
                            Toast.makeText(this, getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(this, getString(R.string.error) + ": " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
