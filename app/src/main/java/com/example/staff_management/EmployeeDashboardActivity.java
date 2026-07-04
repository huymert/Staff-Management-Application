package com.example.staff_management;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.staff_management.utils.FirebaseRepository;
import com.example.staff_management.utils.ProductivityCalculator;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 * EmployeeDashboardActivity:
 * - Dùng để: mở màn hình chính cho nhân viên.
 * - Kiểm tra/xử lý: gom chấm công, xem lương giờ làm, đổi avatar và chuyển màn hình.
 * - Sau đó: nhân viên thao tác nhanh các chức năng cá nhân trong ngày.
 */
public class EmployeeDashboardActivity extends BaseActivity {

    private ImageButton btnLogout, btnSettings;
    private TextView tvUserName, tvUserEmail, tvCheckInTime, tvCheckOutTime, tvTotalHours, tvMainStatus, tvTimer, tvWorkDurationLabel, tvCurrentDate;
    private View btnMainAction;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;
    private ShapeableImageView ivUserAvatar;
    private FirebaseFirestore db;
    private String userId;
    private boolean isCheckedIn = false;
    private long startTime = 0;
    private final android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    /*
     * onCreate:
     * - Dùng để: khởi tạo dashboard của nhân viên.
     * - Kiểm tra/xử lý: lấy user hiện tại, gắn view, set lịch, bottom nav và các nút thao tác.
     * - Sau đó: màn hình sẵn sàng để nhân viên chấm công và xem thông tin cá nhân.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        userId = currentUser.getUid();

        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnMainAction = findViewById(R.id.btnMainAction);
        tvMainStatus = findViewById(R.id.tvMainStatus);
        tvTimer = findViewById(R.id.tvTimer);
        tvWorkDurationLabel = findViewById(R.id.tvWorkDurationLabel);
        tvCheckInTime = findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        View footerContainer = findViewById(R.id.footerContainer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.employeeDashboardRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                android.view.ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int)(150 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerLayout = findViewById(R.id.headerLayout);
            if (headerLayout != null) {
                headerLayout.setPadding(
                    headerLayout.getPaddingLeft(),
                    systemBars.top + (int)(4 * getResources().getDisplayMetrics().density),
                    headerLayout.getPaddingRight(),
                    headerLayout.getPaddingBottom()
                );
            }
            
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

        findViewById(R.id.btnLeaveRequests).setOnClickListener(v ->
                startActivity(new Intent(this, LeaveRequestActivity.class)));

        tvCurrentDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date()));

        setupBottomNavigation();
        setupLaunchers();

        ivUserAvatar.setOnClickListener(v -> showImageOptionsDialog());

        loadUserProfile();
        checkCurrentAttendanceStatus();

        btnMainAction.setOnClickListener(v -> {
            if (!isCheckedIn) {
                performCheckIn();
            } else {
                performCheckOut();
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(EmployeeDashboardActivity.this, LoginActivity.class));
            finish();
        });

        btnSettings.setOnClickListener(v -> showChangePasswordDialog());
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho nhân viên.
     * - Kiểm tra/xử lý: bắt sự kiện home, chat, task và AI chat.
     * - Sau đó: chuyển màn hình đúng chức năng mà nhân viên cần.
     */
    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_messenger) {
                startActivity(new Intent(EmployeeDashboardActivity.this, UserListChatActivity.class));
                return true;
            } else if (itemId == R.id.nav_task) {
                startActivity(new Intent(EmployeeDashboardActivity.this, TaskActivity.class));
                return true;
            } else if (itemId == R.id.nav_ai_chat) {
                showAiAssistantDialog();
                return true;
            }
            return false;
        });
    }

    /*
     * checkCurrentAttendanceStatus:
     * - Dùng để: kiểm tra trạng thái chấm công của hôm nay.
     * - Kiểm tra/xử lý: đọc document Attendance theo ngày hiện tại rồi xem đã check-in hay check-out chưa.
     * - Sau đó: cập nhật lại giao diện đúng với ca làm hiện tại.
     */
    private void checkCurrentAttendanceStatus() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String attendanceId = userId + "_" + today;

        db.collection("Attendance").document(attendanceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    Date checkInDate = documentSnapshot.getDate("checkIn");
                    Date checkOutDate = documentSnapshot.getDate("checkOut");

                    if (checkInDate == null) {
                        return;
                    }

                    tvCheckInTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(checkInDate));
                    if (checkOutDate == null) {
                        startTimer(checkInDate.getTime());
                        updateUIForCheckedIn();
                    } else {
                        tvCheckOutTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(checkOutDate));
                        calculateAndDisplayTotalHours(checkInDate, checkOutDate);
                        updateUIForCheckedOut();
                    }
        });
    }

    /*
     * startTimer:
     * - Dùng để: chạy đồng hồ đếm thời gian làm việc cho nhân viên.
     * - Kiểm tra/xử lý: lấy thời điểm bắt đầu rồi cập nhật text mỗi giây.
     * - Sau đó: nhân viên thấy thời gian làm việc đang tăng lên liên tục.
     */
    private void startTimer(long start) {
        stopTimer();
        startTime = start;
        isCheckedIn = true;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;

                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /*
     * stopTimer:
     * - Dùng để: dừng đồng hồ làm việc.
     * - Kiểm tra/xử lý: remove runnable đang chạy và reset trạng thái check-in.
     * - Sau đó: timer không còn chạy khi đã ra về.
     */
    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        isCheckedIn = false;
    }

    /*
     * updateUIForCheckedIn:
     * - Dùng để: đổi giao diện sau khi đã check-in.
     * - Kiểm tra/xử lý: bật hiển thị timer và nhãn thời lượng làm việc.
     * - Sau đó: màn hình cho thấy nhân viên đang trong ca làm.
     */
    private void updateUIForCheckedIn() {
        tvMainStatus.setText(R.string.status_checked_in);
        tvTimer.setVisibility(View.VISIBLE);
        tvWorkDurationLabel.setVisibility(View.VISIBLE);
    }

    /*
     * updateUIForCheckedOut:
     * - Dùng để: đổi giao diện sau khi đã check-out.
     * - Kiểm tra/xử lý: ẩn timer và nhãn thời lượng làm việc.
     * - Sau đó: màn hình chuyển sang trạng thái đã ra về.
     */
    private void updateUIForCheckedOut() {
        tvMainStatus.setText(R.string.status_checked_out);
        tvTimer.setVisibility(View.GONE);
        tvWorkDurationLabel.setVisibility(View.GONE);
    }

    /*
     * calculateAndDisplayTotalHours:
     * - Dùng để: tính tổng số giờ làm trong ngày.
     * - Kiểm tra/xử lý: lấy chênh lệch giữa giờ check-in và check-out.
     * - Sau đó: hiển thị kết quả theo format giờ và phút.
     */
    private void calculateAndDisplayTotalHours(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        long minutes = diff / (60 * 1000) % 60;
        long hours = diff / (60 * 60 * 1000);
        tvTotalHours.setText(String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes));
    }

    /*
     * loadUserProfile:
     * - Dùng để: lấy thông tin cá nhân của nhân viên.
     * - Kiểm tra/xử lý: đọc tên, email và avatarUrl từ Firestore.
     * - Sau đó: đổ dữ liệu lên header của dashboard.
     */
    private void loadUserProfile() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");

                    if (fullName != null) tvUserName.setText(fullName);
                    if (email != null) tvUserEmail.setText(email);

                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        com.bumptech.glide.Glide.with(this).load(avatarUrl).into(ivUserAvatar);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    /*
     * performCheckIn:
     * - Dùng để: lưu thời gian vào làm của nhân viên.
     * - Kiểm tra/xử lý: tạo mã điểm danh theo ngày rồi ghi dữ liệu check-in lên Firestore.
     * - Sau đó: cập nhật giao diện, bật timer và đổi trạng thái online.
     */
    private void performCheckIn() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        String attendanceId = userId + "_" + today;
        Date now = new Date();

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("userId", userId);
        attendance.put("date", today);
        attendance.put("checkIn", now);
        attendance.put("month", Integer.parseInt(new SimpleDateFormat("MM", Locale.getDefault()).format(now)));
        attendance.put("year", Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(now)));

        db.collection("Attendance").document(attendanceId)
                .set(attendance)
                .addOnSuccessListener(aVoid -> {
                    tvCheckInTime.setText(currentTime);
                    tvCheckOutTime.setText("--:--");
                    startTimer(now.getTime());
                    updateUIForCheckedIn();
                    Toast.makeText(this, "Checked in successfully", Toast.LENGTH_SHORT).show();
                    updateUserStatus("online");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Check-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /*
     * performCheckOut:
     * - Dùng để: lưu thời gian ra về của nhân viên.
     * - Kiểm tra/xử lý: cập nhật checkOut, tính tổng giờ làm và cập nhật trạng thái làm việc.
     * - Sau đó: hiển thị giờ ra về, tắt timer và tính lại năng suất.
     */
    private void performCheckOut() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        String attendanceId = userId + "_" + today;
        Date now = new Date();

        db.collection("Attendance").document(attendanceId)
                .update("checkOut", now)
                .addOnSuccessListener(aVoid -> {
                    tvCheckOutTime.setText(currentTime);
                    stopTimer();
                    updateUIForCheckedOut();

                    db.collection("Attendance").document(attendanceId).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    calculateAndDisplayTotalHours(doc.getDate("checkIn"), now);
                                }
                            });

                    Toast.makeText(this, "Checked out successfully", Toast.LENGTH_SHORT).show();
                    updateUserStatus("offline");
                    ProductivityCalculator.recalculate(userId, db);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Check-out failed. Did you check in today?", Toast.LENGTH_SHORT).show());
    }

    /*
     * updateUserStatus:
     * - Dùng để: cập nhật trạng thái online/offline của nhân viên.
     * - Kiểm tra/xử lý: gọi repository để ghi lại presenceStatus trên Firestore.
     * - Sau đó: các màn chat và danh sách nhân sự thấy được trạng thái mới.
     */
    private void updateUserStatus(String status) {
        FirebaseRepository.updatePresence(userId, status);
    }

    /*
     * setupLaunchers:
     * - Dùng để: đăng ký launcher cho chọn ảnh từ gallery hoặc camera.
     * - Kiểm tra/xử lý: nhận kết quả trả về rồi đẩy ảnh sang luồng upload.
     * - Sau đó: nhân viên có thể đổi avatar nhanh từ hai nguồn ảnh.
     */
    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                uploadImageToFirebase(imageUri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                if (photo == null) {
                    Toast.makeText(this, "Khong the lay anh tu camera", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri imageUri = getImageUri(photo);
                uploadImageToFirebase(imageUri);
            }
        });
    }

    /*
     * showImageOptionsDialog:
     * - Dùng để: mở dialog chọn nguồn ảnh đại diện.
     * - Kiểm tra/xử lý: hiển thị ảnh hiện tại rồi cho chọn gallery hoặc camera.
     * - Sau đó: gọi launcher tương ứng để lấy ảnh mới.
     */
    private void showImageOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_options, null);
        builder.setView(dialogView);

        ShapeableImageView ivZoomedImage = dialogView.findViewById(R.id.ivZoomedImage);
        com.google.android.material.button.MaterialButton btnGallery = dialogView.findViewById(R.id.btnGallery);
        com.google.android.material.button.MaterialButton btnCamera = dialogView.findViewById(R.id.btnCamera);

        ivZoomedImage.setImageDrawable(ivUserAvatar.getDrawable());

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
     * - Dùng để: lưu ảnh đại diện mới lên Firebase Storage và Firestore.
     * - Kiểm tra/xử lý: upload file, lấy URL, rồi cập nhật lại document user.
     * - Sau đó: avatar mới được hiển thị trên giao diện.
     */
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null || userId == null) {
            Toast.makeText(this, "Khong co quyen tai anh hoac anh loi", Toast.LENGTH_SHORT).show();
            return;
        }

        View loadingOverlay = findViewById(R.id.loadingOverlay);
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

        String bucketUrl = "gs://staff-management-953fe.firebasestorage.app";
        com.google.firebase.storage.StorageReference fileRef = com.google.firebase.storage.FirebaseStorage.getInstance(bucketUrl).getReference()
                .child("avatars/" + userId + ".jpg");

        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Khong the mo tep tin", Toast.LENGTH_SHORT).show();
                return;
            }

            fileRef.putStream(inputStream)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        db.collection("Users").document(userId)
                                .update("avatarUrl", downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                                    com.bumptech.glide.Glide.with(this).load(downloadUrl).into(ivUserAvatar);
                                    Toast.makeText(this, "Anh dai dien da duoc cap nhat", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                                    Toast.makeText(this, "Cap nhat Firestore that bai: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }))
                    .addOnFailureListener(e -> {
                        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(this, "Tai anh len that bai: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (java.io.FileNotFoundException e) {
            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Khong tim thay tep tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * getImageUri:
     * - Dùng để: đổi bitmap từ camera thành Uri.
     * - Kiểm tra/xử lý: nén ảnh rồi lưu tạm vào MediaStore.
     * - Sau đó: có Uri để đưa vào hàm upload.
     */
    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Avatar_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }

    /*
     * onDestroy:
     * - Dùng để: dọn timer khi màn hình bị đóng.
     * - Kiểm tra/xử lý: gọi stopTimer trước khi hủy activity.
     * - Sau đó: tránh chạy task ngầm và tránh tốn tài nguyên.
     */
    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }
}
