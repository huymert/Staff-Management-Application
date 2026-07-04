package com.example.staff_management;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.example.staff_management.utils.LocaleHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/*
 * BaseActivity:
 * - Dùng để: làm lớp gốc cho các màn hình trong app.
 * - Kiểm tra/xử lý: xử lý ngôn ngữ, trạng thái online/offline, đăng nhập và đổi mật khẩu.
 * - Sau đó: các Activity con dùng lại logic chung cho gọn hơn.
 */
public class BaseActivity extends AppCompatActivity {

    private AiAssistant aiAssistant;

    /*
     * onCreate:
     * - Dùng để: bật chế độ EdgeToEdge cho toàn app.
     * - Kiểm tra/xử lý: gọi cấu hình giao diện trước khi Activity con chạy tiếp.
     * - Sau đó: màn hình con có layout sát viền hơn và nhìn hiện đại hơn.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    /*
     * attachBaseContext:
     * - Dùng để: gắn lại context theo ngôn ngữ đã chọn.
     * - Kiểm tra/xử lý: lấy locale từ LocaleHelper rồi wrap vào base context.
     * - Sau đó: toàn bộ màn hình con mở lên đúng ngôn ngữ người dùng chọn.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    /*
     * requireAuthenticatedUser:
     * - Dùng để: kiểm tra user đã đăng nhập hay chưa.
     * - Kiểm tra/xử lý: lấy FirebaseUser hiện tại, nếu null thì chuyển về login.
     * - Sau đó: chỉ user hợp lệ mới được đi tiếp vào các màn chính.
     */
    protected FirebaseUser requireAuthenticatedUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            navigateToLoginAndFinish();
        }
        return user;
    }

    /*
     * navigateToLoginAndFinish:
     * - Dùng để: đưa user về màn hình đăng nhập.
     * - Kiểm tra/xử lý: clear task cũ rồi mở LoginActivity.
     * - Sau đó: user không quay lại được màn đang bị khóa.
     */
    protected void navigateToLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /*
     * showLanguageChooser:
     * - Dùng để: mở hộp thoại chọn ngôn ngữ.
     * - Kiểm tra/xử lý: lấy ngôn ngữ hiện tại rồi đổi sang tiếng Việt hoặc tiếng Anh.
     * - Sau đó: app reload để giao diện hiển thị đúng ngôn ngữ mới.
     */
    protected void showLanguageChooser() {
        String[] langs = {getString(R.string.language_vietnamese), getString(R.string.language_english)};
        String current = LocaleHelper.getLanguage(this);
        int checkedItem = "vi".equals(current) ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(langs, checkedItem, (dialog, which) -> {
                    String selected = which == 0 ? "vi" : "en";
                    LocaleHelper.setLocale(this, selected);
                    dialog.dismiss();
                    restartAppForLocale();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /*
     * restartAppForLocale:
     * - Dùng để: khởi động lại app sau khi đổi ngôn ngữ.
     * - Kiểm tra/xử lý: mở lại MainActivity và xóa task cũ.
     * - Sau đó: toàn bộ UI load lại theo locale mới.
     */
    protected void restartAppForLocale() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /*
     * onResume:
     * - Dùng để: đánh dấu user đang online khi app trở lại foreground.
     * - Kiểm tra/xử lý: gọi update presence với trạng thái online.
     * - Sau đó: các màn chat và danh sách sẽ thấy user đang hoạt động.
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateUserPresence("online");
    }

    /*
     * onPause:
     * - Dùng để: đánh dấu user offline khi app tạm dừng.
     * - Kiểm tra/xử lý: cập nhật presence sang offline.
     * - Sau đó: hệ thống hiểu là user không còn hoạt động trên màn hình.
     */
    @Override
    protected void onPause() {
        super.onPause();
        updateUserPresence("offline");
    }

    /*
     * updateUserPresence:
     * - Dùng để: lưu trạng thái online/offline của user.
     * - Kiểm tra/xử lý: lấy user hiện tại rồi update field presenceStatus trên Firestore.
     * - Sau đó: dữ liệu trạng thái được đồng bộ cho các màn khác dùng chung.
     */
    private void updateUserPresence(String status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.example.staff_management.utils.FirebaseRepository.updatePresence(user.getUid(), status);
        }
    }

    /*
     * showChangePasswordDialog:
     * - Dùng để: mở hộp thoại đổi mật khẩu.
     * - Kiểm tra/xử lý: nhập mật khẩu cũ, mật khẩu mới và xác nhận rồi kiểm tra hợp lệ.
     * - Sau đó: reauthenticate xong thì lưu mật khẩu mới lên Firebase Auth.
     */
    protected void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        android.widget.EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        android.widget.EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        android.widget.EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        View btnUpdate = dialogView.findViewById(R.id.btnUpdatePassword);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(dialogView, 0, 0, 0, 0);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(view -> {
            String oldPass = etOldPassword.getText() != null ? etOldPassword.getText().toString() : "";
            String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
            String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_fill_all), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
                user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(this, getString(R.string.change_password_success), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "";
                                Toast.makeText(this, getString(R.string.change_password_failed, error), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, getString(R.string.reauth_failed), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    /*
     * showAiAssistantDialog:
     * - Dùng để: mở khung chat AI ở dạng bottom sheet.
     * - Kiểm tra/xử lý: đẩy bottom sheet lên trạng thái mở rộng và gửi câu hỏi cho AI assistant.
     * - Sau đó: hiển thị câu trả lời hoặc lỗi ngay trên màn hình.
     */
    @SuppressLint("InflateParams")
    protected void showAiAssistantDialog() {
        if (aiAssistant == null) {
            aiAssistant = new AiAssistant();
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_ai_chat, null);
        dialog.setContentView(view);

        dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        TextView tvAiResponse = view.findViewById(R.id.tvAiResponse);
        TextInputEditText etUserQuery = view.findViewById(R.id.etUserQuery);
        View btnSend = view.findViewById(R.id.btnSendQuery);
        android.widget.ProgressBar pbLoading = view.findViewById(R.id.pbLoading);

        btnSend.setOnClickListener(v -> {
            String query = etUserQuery.getText() != null ? etUserQuery.getText().toString().trim() : "";
            if (query.isEmpty()) {
                return;
            }

            pbLoading.setVisibility(View.VISIBLE);
            tvAiResponse.setText("AI dang suy nghi...");
            etUserQuery.setText("");

            aiAssistant.ask(query, new AiAssistant.AiCallback() {
                @Override
                public void onResponse(String response) {
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        tvAiResponse.setText(response);
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        tvAiResponse.setText("Loi: " + t.getMessage());
                    });
                }
            });
        });

        dialog.show();
    }
}
