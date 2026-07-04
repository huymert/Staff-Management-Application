package com.example.staff_management;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.example.staff_management.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;

import com.example.staff_management.utils.LocaleHelper;
import android.content.Context;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

/*
 * AdminDashboardActivity:
 * - Dùng để: mở dashboard chính cho admin.
 * - Kiểm tra/xử lý: gom các chức năng quản lý nhân viên, lương, chấm công, task, đơn nghỉ và chat.
 * - Sau đó: admin chuyển màn hình nhanh tới đúng module mình cần.
 */
public class AdminDashboardActivity extends BaseActivity {

    private MaterialCardView cardAddStaff, cardManageSalary, cardMonitorStatus, cardProductivity, cardLeaveRequests;
    private ImageView btnLogout, btnAdminSettings;
    private ShapeableImageView ivAdminAvatar;
    private TextView tvAdminName, tvAdminEmail;
    private BottomNavigationView bottomNavigation;
    private View mainContent;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private FirebaseFirestore db;
    private String userId;
    private List<User> employeeList = new ArrayList<>();
    private AiAssistant aiAssistant;
    private ListenerRegistration employeeListListener;

    /*
     * onCreate:
     * - Dùng để: khởi tạo dashboard admin và gắn các view chính.
     * - Kiểm tra/xử lý: lấy dữ liệu user, cấu hình bottom nav, launcher ảnh và các card chức năng.
     * - Sau đó: hiển thị dashboard và cho admin thao tác các chức năng quản lý.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        ivAdminAvatar = findViewById(R.id.ivAdminAvatar);
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        cardAddStaff = findViewById(R.id.cardAddStaff);
        cardManageSalary = findViewById(R.id.cardManageSalary);
        cardMonitorStatus = findViewById(R.id.cardMonitorStatus);
        cardProductivity = findViewById(R.id.cardProductivity);
        cardLeaveRequests = findViewById(R.id.cardLeaveRequests);
        btnAdminSettings = findViewById(R.id.btnAdminSettings);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        mainContent = findViewById(R.id.mainContent);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootDashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            View headerArea = findViewById(R.id.headerArea);
            if (headerArea != null) {
                android.view.ViewGroup.LayoutParams lp = headerArea.getLayoutParams();
                lp.height = (int)(150 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerArea.setLayoutParams(lp);
            }

            View topHeaderContent = findViewById(R.id.topHeaderContent);
            if (topHeaderContent != null) {
                topHeaderContent.setPadding(
                    topHeaderContent.getPaddingLeft(),
                    systemBars.top + (int)(4 * getResources().getDisplayMetrics().density),
                    topHeaderContent.getPaddingRight(),
                    topHeaderContent.getPaddingBottom()
                );
            }
            
            View footerContainer = findViewById(R.id.footerContainer);
            if (footerContainer != null) {
                footerContainer.setPadding(
                    footerContainer.getPaddingLeft(),
                    footerContainer.getPaddingTop(),
                    footerContainer.getPaddingRight(),
                    systemBars.bottom + (int)(8 * getResources().getDisplayMetrics().density)
                );
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        db = FirebaseFirestore.getInstance();
        aiAssistant = new AiAssistant();
        fetchEmployeeList();
        
        setupBottomNavigation();
        
        FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        userId = currentUser.getUid();
        loadAdminProfile();

        ivAdminAvatar.setOnClickListener(v -> showImageOptionsDialog());

        galleryLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                uploadImageToFirebase(imageUri);
            }
        });

        cameraLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                android.graphics.Bitmap photo = (android.graphics.Bitmap) result.getData().getExtras().get("data");
                if (photo == null) {
                    Toast.makeText(this, "KhÃ´ng thá»ƒ láº¥y áº£nh tá»« camera", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri imageUri = getImageUri(photo);
                uploadImageToFirebase(imageUri);
            }
        });

        cardAddStaff.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, EmployeeListActivity.class);
            intent.putExtra("mode", "management");
            startActivity(intent);
        });

        cardManageSalary.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, SalaryCalculationActivity.class));
        });

        cardMonitorStatus.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, EmployeeListActivity.class);
            intent.putExtra("mode", "attendance_view");
            startActivity(intent);
        });

        cardProductivity.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, EmployeeListActivity.class);
            intent.putExtra("mode", "productivity");
            startActivity(intent);
        });

        cardLeaveRequests.setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, LeaveRequestActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
            finish();
        });

        btnAdminSettings.setOnClickListener(v -> showChangePasswordDialog());
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho admin.
     * - Kiểm tra/xử lý: bắt sự kiện từng tab để mở home, chat, thống kê hoặc AI chat.
     * - Sau đó: admin chuyển nhanh sang màn hình cần dùng.
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                mainContent.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_messenger) {
                Intent intent = new Intent(this, UserListChatActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_stats) {
                Intent intent = new Intent(this, StatisticsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_ai_chat) {
                showAiChatDialog();
                return true;
            }
            return false;
        });
    }

    /*
     * fetchEmployeeList:
     * - Dùng để: lấy danh sách nhân viên để phục vụ các màn hình quản lý.
     * - Kiểm tra/xử lý: nghe realtime collection Users và bỏ qua tài khoản admin.
     * - Sau đó: employeeList luôn được cập nhật theo dữ liệu mới nhất.
     */
    private void fetchEmployeeList() {
        employeeListListener = db.collection("Users")
                .whereNotEqualTo("role", "admin")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    employeeList.clear();
                    if (value != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            employeeList.add(doc.toObject(User.class));
                        }
                    }
                });
    }

    /*
     * showAiChatDialog:
     * - Dùng để: mở khung chat AI ngay trong dashboard.
     * - Kiểm tra/xử lý: lấy câu hỏi từ người dùng rồi gửi sang assistant để xử lý.
     * - Sau đó: hiển thị câu trả lời ngay trên dialog.
     */
    @SuppressLint("InflateParams")
    private void showAiChatDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_ai_chat, null);
        dialog.setContentView(view);

        dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        TextView tvAiResponse = view.findViewById(R.id.tvAiResponse);
        TextInputEditText etUserQuery = view.findViewById(R.id.etUserQuery);
        View btnSend = view.findViewById(R.id.btnSendQuery);
        android.widget.ProgressBar pbLoading = view.findViewById(R.id.pbLoading);

        btnSend.setOnClickListener(v -> {
            String query = etUserQuery.getText().toString().trim();
            if (query.isEmpty()) return;

            pbLoading.setVisibility(View.VISIBLE);
            tvAiResponse.setText("AI đang suy nghĩ...");
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
                        tvAiResponse.setText("Lỗi: " + t.getMessage());
                    });
                }
            });
        });

        dialog.show();
    }

    /*
     * loadAdminProfile:
     * - Dùng để: lấy thông tin hồ sơ của admin từ Firestore.
     * - Kiểm tra/xử lý: đọc tên, email và avatarUrl rồi gắn lên giao diện.
     * - Sau đó: header dashboard hiển thị đúng thông tin tài khoản đang đăng nhập.
     */
    private void loadAdminProfile() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        
                        if (fullName != null) tvAdminName.setText(fullName);
                        if (email != null) tvAdminEmail.setText(email);

                        String avatarUrl = documentSnapshot.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this).load(avatarUrl).into(ivAdminAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    /*
     * showImageOptionsDialog:
     * - Dùng để: mở hộp thoại chọn ảnh đại diện mới cho admin.
     * - Kiểm tra/xử lý: cho xem ảnh hiện tại rồi chọn nguồn từ gallery hoặc camera.
     * - Sau đó: gọi đúng launcher để lấy ảnh mới.
     */
    private void showImageOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_options, null);
        builder.setView(dialogView);

        ShapeableImageView ivZoomedImage = dialogView.findViewById(R.id.ivZoomedImage);
        com.google.android.material.button.MaterialButton btnGallery = dialogView.findViewById(R.id.btnGallery);
        com.google.android.material.button.MaterialButton btnCamera = dialogView.findViewById(R.id.btnCamera);

        ivZoomedImage.setImageDrawable(ivAdminAvatar.getDrawable());

        AlertDialog dialog = builder.create();

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
            dialog.dismiss();
        });

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    /*
     * uploadImageToFirebase:
     * - Dùng để: lưu ảnh đại diện mới lên Firebase Storage.
     * - Kiểm tra/xử lý: upload file, lấy URL tải về rồi cập nhật Firestore.
     * - Sau đó: avatar trên giao diện đổi sang ảnh mới và ẩn loading.
     */
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null || userId == null) {
            Toast.makeText(this, "Không có quyền tải ảnh hoặc ảnh lỗi", Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View loadingOverlay = findViewById(R.id.loadingOverlay);
        if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.VISIBLE);

        String bucketUrl = "gs://staff-management-953fe.firebasestorage.app";
        com.google.firebase.storage.StorageReference fileRef = com.google.firebase.storage.FirebaseStorage.getInstance(bucketUrl).getReference()
                .child("avatars/" + userId + ".jpg");

        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.GONE);
                Toast.makeText(this, "Không thể mở tệp tin", Toast.LENGTH_SHORT).show();
                return;
            }

            fileRef.putStream(inputStream)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        db.collection("Users").document(userId)
                                .update("avatarUrl", downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.GONE);
                                    com.bumptech.glide.Glide.with(this).load(downloadUrl).into(ivAdminAvatar);
                                    Toast.makeText(this, "Ảnh đại diện Admin đã được cập nhật", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.GONE);
                                    Toast.makeText(this, "Cập nhật Firestore thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }))
                    .addOnFailureListener(e -> {
                        if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.GONE);
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("Object does not exist")) {
                            errorMsg = "Lỗi: Object does not exist (404). Kiểm tra xem Bucket đã được tạo trên Console chưa?";
                        }
                        Toast.makeText(this, "Tải ảnh lên thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                        android.util.Log.e("FirebaseStorage", "Upload failed", e);
                    });
        } catch (java.io.FileNotFoundException e) {
            if (loadingOverlay != null) loadingOverlay.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Không tìm thấy tệp tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * onDestroy:
     * - Dùng để: dọn listener Firestore khi màn hình bị đóng.
     * - Kiểm tra/xử lý: remove listener đang theo dõi danh sách nhân viên.
     * - Sau đó: tránh leak và tránh nghe dữ liệu nền không cần thiết.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (employeeListListener != null) employeeListListener.remove();
    }

    /*
     * getImageUri:
     * - Dùng để: đổi ảnh chụp từ camera sang Uri.
     * - Kiểm tra/xử lý: nén bitmap rồi lưu tạm vào MediaStore.
     * - Sau đó: có Uri để đưa tiếp vào luồng upload ảnh.
     */
    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Avatar_Admin_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }
}
