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
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
 * ManagerDashboardActivity:
 * - Dùng để: mở màn hình chính cho manager.
 * - Kiểm tra/xử lý: gom chấm công, xem nhân viên, xem task, đổi ảnh đại diện và chuyển màn hình.
 * - Sau đó: manager thao tác nhanh các chức năng quản lý trong ngày.
 */
public class ManagerDashboardActivity extends BaseActivity {

    private ImageButton btnLogout, btnSettings;
    private ShapeableImageView ivUserAvatar;
    private TextView tvUserName, tvUserEmail, tvCheckInTime, tvCheckOutTime, tvTotalHours, tvMainStatus, tvTimer, tvWorkDurationLabel, tvCurrentDate;
    private View btnMainAction;
    private BottomNavigationView bottomNavigation;
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
     * - Dùng để: khởi tạo dashboard của manager.
     * - Kiểm tra/xử lý: lấy user hiện tại, gắn view, set lịch, set button và cấu hình thanh điều hướng.
     * - Sau đó: hiển thị giao diện chính để manager chấm công và đi tới các màn khác.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        findViewById(R.id.headerBackground).setBackgroundResource(R.drawable.bg_header_manager);

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.managerDashboardRoot), (v, insets) -> {
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

        loadUserProfile();
        checkCurrentAttendanceStatus();
        setupBottomNavigation();
        setupLaunchers();

        ivUserAvatar.setOnClickListener(v -> showImageOptionsDialog());

        btnMainAction.setOnClickListener(v -> {
            if (!isCheckedIn) {
                performCheckIn();
            } else {
                performCheckOut();
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnSettings.setOnClickListener(v -> showChangePasswordDialog());
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho manager.
     * - Kiểm tra/xử lý: bắt sự kiện home, chat, team, task và AI chat.
     * - Sau đó: chuyển đúng màn hình theo từng tab người dùng chọn.
     */
    private void setupBottomNavigation() {
        bottomNavigation.getMenu().clear();
        bottomNavigation.inflateMenu(R.menu.manager_bottom_nav_menu);
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_messenger) {
                startActivity(new Intent(this, UserListChatActivity.class));
                return true;
            } else if (id == R.id.nav_my_team) {
                Intent intent = new Intent(this, EmployeeListActivity.class);
                intent.putExtra("mode", "productivity");
                intent.putExtra("role", "manager");
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_task) {
                Intent intent = new Intent(this, ManagerTaskActivity.class);
                intent.putExtra("role", "manager");
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_ai_chat) {
                showAiAssistantDialog();
                return true;
            }
            return false;
        });
    }

    /*
     * performCheckIn:
     * - Dùng để: lưu thời gian vào làm của manager.
     * - Kiểm tra/xử lý: tạo mã điểm danh theo ngày rồi ghi dữ liệu check-in lên Firestore.
     * - Sau đó: cập nhật giao diện, bật timer và đổi trạng thái online.
     */
    private void performCheckIn() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String attendanceId = userId + "_" + today;
        Date now = new Date();
        Map<String, Object> attendance = new HashMap<>();
        attendance.put("userId", userId);
        attendance.put("date", today);
        attendance.put("checkIn", now);
        attendance.put("month", Integer.parseInt(new SimpleDateFormat("MM", Locale.getDefault()).format(now)));
        attendance.put("year", Integer.parseInt(new SimpleDateFormat("yyyy", Locale.getDefault()).format(now)));

        db.collection("Attendance").document(attendanceId).set(attendance).addOnSuccessListener(aVoid -> {
            tvCheckInTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now));
            tvCheckOutTime.setText("--:--");
            startTimer(now.getTime());
            updateUIForCheckedIn();
            updateUserStatus("online");
            Toast.makeText(this, "Checked in successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(this, "Check-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /*
     * performCheckOut:
     * - Dùng để: lưu thời gian ra về của manager.
     * - Kiểm tra/xử lý: cập nhật checkOut, tính tổng giờ làm và cập nhật trạng thái làm việc.
     * - Sau đó: hiển thị giờ ra về, tắt timer và tính lại năng suất.
     */
    private void performCheckOut() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String attendanceId = userId + "_" + today;
        Date now = new Date();
        db.collection("Attendance").document(attendanceId).update("checkOut", now).addOnSuccessListener(aVoid -> {
            tvCheckOutTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now));
            stopTimer();
            updateUIForCheckedOut();
            db.collection("Attendance").document(attendanceId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) calculateAndDisplayTotalHours(doc.getDate("checkIn"), now);
            });
            updateUserStatus("offline");
            ProductivityCalculator.recalculate(userId, db);
            Toast.makeText(this, "Checked out successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(this, "Check-out failed. Did you check in today?", Toast.LENGTH_SHORT).show());
    }

    /*
     * startTimer:
     * - Dùng để: chạy đồng hồ đếm thời gian làm việc.
     * - Kiểm tra/xử lý: lấy thời điểm bắt đầu rồi cập nhật text mỗi giây.
     * - Sau đó: manager thấy được thời gian làm việc đang chạy liên tục.
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
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /*
     * stopTimer:
     * - Dùng để: dừng đồng hồ làm việc.
     * - Kiểm tra/xử lý: remove runnable đang chạy và reset trạng thái check-in.
     * - Sau đó: timer không còn tăng nữa khi đã check-out.
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
     * - Kiểm tra/xử lý: bật các phần hiển thị thời gian làm việc và trạng thái đang làm.
     * - Sau đó: màn hình cho thấy user đang trong ca làm.
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
     * checkCurrentAttendanceStatus:
     * - Dùng để: kiểm tra trạng thái chấm công của ngày hiện tại.
     * - Kiểm tra/xử lý: đọc document Attendance theo ngày hôm nay rồi xem đã check-in hay check-out chưa.
     * - Sau đó: cập nhật lại giao diện đúng với trạng thái hiện tại.
     */
    private void checkCurrentAttendanceStatus() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        db.collection("Attendance").document(userId + "_" + today).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getDate("checkIn") != null) {
                Date in = doc.getDate("checkIn");
                tvCheckInTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(in));
                if (doc.getDate("checkOut") == null) {
                    startTimer(in.getTime());
                    updateUIForCheckedIn();
                } else {
                    Date out = doc.getDate("checkOut");
                    tvCheckOutTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(out));
                    calculateAndDisplayTotalHours(in, out);
                    updateUIForCheckedOut();
                }
            }
        });
    }

    /*
     * updateUserStatus:
     * - Dùng để: cập nhật trạng thái online/offline của manager.
     * - Kiểm tra/xử lý: gọi repository để ghi lại presenceStatus trên Firestore.
     * - Sau đó: các màn chat và danh sách nhân sự thấy được trạng thái mới.
     */
    private void updateUserStatus(String status) {
        FirebaseRepository.updatePresence(userId, status);
    }

    /*
     * loadUserProfile:
     * - Dùng để: lấy thông tin cá nhân của manager.
     * - Kiểm tra/xử lý: đọc tên, email và avatarUrl từ Firestore.
     * - Sau đó: đổ dữ liệu lên header của dashboard.
     */
    private void loadUserProfile() {
        if (userId == null) return;
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvUserName.setText(doc.getString("fullName"));
                        tvUserEmail.setText(doc.getString("email"));
                        String url = doc.getString("avatarUrl");
                        if (url != null && !url.isEmpty()) {
                            com.bumptech.glide.Glide.with(this).load(url).into(ivUserAvatar);
                        }
                    }
                });
    }

    /*
     * setupLaunchers:
     * - Dùng để: đăng ký launcher cho chọn ảnh từ gallery hoặc camera.
     * - Kiểm tra/xử lý: nhận kết quả trả về rồi đẩy ảnh sang luồng upload.
     * - Sau đó: manager có thể đổi avatar nhanh từ hai nguồn ảnh.
     */
    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) uploadImageToFirebase(result.getData().getData());
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                if (photo == null) {
                    Toast.makeText(this, "Khong the lay anh tu camera", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadImageToFirebase(getImageUri(photo));
            }
        });
    }

    /*
     * showImageOptionsDialog:
     * - Dùng để: mở dialog chọn nguồn ảnh đại diện.
     * - Kiểm tra/xử lý: hiển thị ảnh hiện tại rồi cho chọn gallery hoặc camera.
     * - Sau đó: gọi launcher tương ứng để lấy ảnh mới.
     */
    /*
     * showImageOptionsDialog:
     * - Dùng để: mở dialog chọn nguồn ảnh đại diện.
     * - Kiểm tra/xử lý: hiển thị ảnh hiện tại rồi cho chọn gallery hoặc camera.
     * - Sau đó: gọi launcher tương ứng để lấy ảnh mới.
     */
    private void showImageOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_image_options, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnGallery).setOnClickListener(v -> { galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)); dialog.dismiss(); });
        view.findViewById(R.id.btnCamera).setOnClickListener(v -> { cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); dialog.dismiss(); });
        dialog.show();
    }

    /*
     * uploadImageToFirebase:
     * - Dùng để: lưu ảnh đại diện mới lên Firebase Storage và Firestore.
     * - Kiểm tra/xử lý: upload file, lấy URL, rồi cập nhật lại document user.
     * - Sau đó: avatar mới được hiển thị trên giao diện.
     */
    /*
     * uploadImageToFirebase:
     * - Dùng để: lưu ảnh đại diện mới lên Firebase Storage và Firestore.
     * - Kiểm tra/xử lý: upload file, lấy URL, rồi cập nhật lại document user.
     * - Sau đó: avatar mới được hiển thị trên giao diện.
     */
    private void uploadImageToFirebase(Uri uri) {
        if (uri == null || userId == null) return;
        View loadingOverlay = findViewById(R.id.loadingOverlay);
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
        String bucketUrl = "gs://staff-management-953fe.firebasestorage.app";
        com.google.firebase.storage.StorageReference ref = com.google.firebase.storage.FirebaseStorage.getInstance(bucketUrl).getReference().child("avatars/" + userId + ".jpg");
        ref.putFile(uri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
            db.collection("Users").document(userId).update("avatarUrl", downloadUri.toString()).addOnSuccessListener(aVoid -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
                com.bumptech.glide.Glide.with(this).load(downloadUri).into(ivUserAvatar);
            });
        })).addOnFailureListener(e -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
        });
    }

    /*
     * getImageUri:
     * - Dùng để: đổi bitmap từ camera thành Uri.
     * - Kiểm tra/xử lý: nén ảnh rồi lưu tạm vào MediaStore.
     * - Sau đó: có Uri để đưa vào hàm upload.
     */
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

    @Override
    /*
     * onDestroy:
     * - Dùng để: dọn timer khi màn hình bị đóng.
     * - Kiểm tra/xử lý: gọi stopTimer trước khi hủy activity.
     * - Sau đó: tránh chạy task ngầm và tránh tốn tài nguyên.
     */
    /*
     * onDestroy:
     * - Dùng để: dọn timer khi màn hình bị đóng.
     * - Kiểm tra/xử lý: gọi stopTimer trước khi hủy activity.
     * - Sau đó: tránh chạy task ngầm và tránh tốn tài nguyên.
     */
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }
}
