package com.example.staff_management;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * AttendanceActivity:
 * - Dùng để: mở màn hình chuyên cần và xem lịch điểm danh theo tháng.
 * - Kiểm tra/xử lý: lấy dữ liệu check-in/check-out từ Firestore theo tháng hiện tại.
 * - Sau đó: hiển thị lịch cá nhân hoặc lịch team tùy role.
 */
public class AttendanceActivity extends BaseActivity {

    private ImageButton btnBack, btnPrevMonth, btnNextMonth;
    private TextView tvMonthYear, tvSelectedDate, tvDetailCheckIn, tvDetailCheckOut;
    private RecyclerView rvCalendar;
    private Calendar calendar;
    private List<CalendarDay> calendarDays;
    private CalendarAdapter adapter;
    private FirebaseFirestore db;
    private String userId, userRole;
    private Map<String, AttendanceData> attendanceMap = new HashMap<>();

    /*
     * onCreate:
     * - Dùng để: khởi tạo màn hình chấm công.
     * - Kiểm tra/xử lý: kết nối Firestore, ánh xạ view và set lịch theo role.
     * - Sau đó: hiển thị lịch tháng hiện tại và gắn bottom nav.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        userId = currentUser.getUid();
        userRole = getIntent().getStringExtra("role");

        btnBack = findViewById(R.id.btnBack);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDetailCheckIn = findViewById(R.id.tvDetailCheckIn);
        tvDetailCheckOut = findViewById(R.id.tvDetailCheckOut);
        rvCalendar = findViewById(R.id.rvCalendar);

        View rootLayout = findViewById(R.id.attendance_root_layout);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                View headerBg = findViewById(R.id.headerBackground);
                if (headerBg != null) {
                    headerBg.getLayoutParams().height = (int) (100 * getResources().getDisplayMetrics().density) + statusBarHeight;
                }
                
                btnBack.setTranslationY(statusBarHeight / 2f);
                TextView tvTitle = findViewById(R.id.tvTitle);
                if (tvTitle != null) tvTitle.setTranslationY(statusBarHeight / 2f);
                
                View btnTeam = findViewById(R.id.btnTeamAttendance);
                if (btnTeam != null) btnTeam.setTranslationY(statusBarHeight / 2f);
                
                return insets;
            });
        }

        if ("manager".equalsIgnoreCase(userRole)) {
            applyManagerTheme();
            setupManagerSpecificUI();
        }

        calendar = Calendar.getInstance();
        calendarDays = new ArrayList<>();
        adapter = new CalendarAdapter(calendarDays);
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendar.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        updateCalendar();
        setupBottomNavigation();
    }

    /*
     * setupManagerSpecificUI:
     * - Dùng để: bật thêm phần riêng cho Manager.
     * - Kiểm tra/xử lý: đổi tiêu đề và tạo nút xem chuyên cần team.
     * - Sau đó: manager có thể mở danh sách nhân viên để xem điểm danh.
     */
    private void setupManagerSpecificUI() {
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("Chuyên cần của tôi");

        ImageButton btnTeamAttendance = new ImageButton(this);
        btnTeamAttendance.setId(R.id.btnTeamAttendance);
        btnTeamAttendance.setImageResource(android.R.drawable.ic_menu_mylocation);
        btnTeamAttendance.setBackgroundResource(android.R.drawable.screen_background_light_transparent);
        btnTeamAttendance.setColorFilter(Color.WHITE);
        
        androidx.constraintlayout.widget.ConstraintLayout layout = findViewById(R.id.attendance_root_layout);
        if (layout != null) {
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)
            );
            params.topToTop = R.id.headerBackground;
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
            params.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
            btnTeamAttendance.setLayoutParams(params);
            layout.addView(btnTeamAttendance);

            btnTeamAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(this, EmployeeListActivity.class);
                intent.putExtra("mode", "attendance_view");
                intent.putExtra("role", "manager");
                startActivity(intent);
            });
        }
    }

    /*
     * applyManagerTheme:
     * - Dùng để: đổi màu giao diện cho Manager.
     * - Kiểm tra/xử lý: set màu header, nút và menu bottom nav.
     * - Sau đó: màn hình nhìn đúng theme của manager.
     */
    private void applyManagerTheme() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        findViewById(R.id.headerBackground).setBackgroundColor(getResources().getColor(R.color.blue_primary));
        btnPrevMonth.setColorFilter(getResources().getColor(R.color.blue_primary));
        btnNextMonth.setColorFilter(getResources().getColor(R.color.blue_primary));
        tvMonthYear.setTextColor(getResources().getColor(R.color.blue_dark));
        tvDetailCheckIn.setTextColor(getResources().getColor(R.color.blue_primary));
        tvDetailCheckOut.setTextColor(getResources().getColor(R.color.blue_primary));
        bottomNavigation.getMenu().clear();
        bottomNavigation.inflateMenu(R.menu.manager_bottom_nav_menu);
    }

    /*
     * isManagerRole:
     * - Dùng để: kiểm tra user hiện tại có phải manager không.
     * - Kiểm tra/xử lý: so role đang lưu trong intent.
     * - Sau đó: các phần giao diện sẽ chọn đúng kiểu hiển thị.
     */
    private boolean isManagerRole() {
        return "manager".equalsIgnoreCase(userRole);
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình menu dưới theo role.
     * - Kiểm tra/xử lý: admin thì ẩn, manager và employee thì hiện menu riêng.
     * - Sau đó: các nút home, chat, task và AI đều điều hướng đúng.
     */
    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        
        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                userRole = documentSnapshot.getString("role");
                if ("manager".equalsIgnoreCase(userRole)) {
                    applyManagerTheme();
                } else if ("admin".equalsIgnoreCase(userRole)) {
                    bottomNavigation.setVisibility(View.GONE);
                } else {
                    bottomNavigation.getMenu().clear();
                    bottomNavigation.inflateMenu(R.menu.employee_bottom_nav_menu);
                }
                
                bottomNavigation.setSelectedItemId(R.id.nav_home);
                bottomNavigation.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.nav_home) {
                        Intent intent;
                        if ("manager".equalsIgnoreCase(userRole)) {
                            intent = new Intent(this, ManagerDashboardActivity.class);
                        } else if ("admin".equalsIgnoreCase(userRole)) {
                            intent = new Intent(this, AdminDashboardActivity.class);
                        } else {
                            intent = new Intent(this, EmployeeDashboardActivity.class);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (id == R.id.nav_messenger) {
                        startActivity(new Intent(this, UserListChatActivity.class));
                        finish();
                        return true;
                    } else if (id == R.id.nav_task || id == R.id.nav_my_team) {
                        Intent intent;
                        if (id == R.id.nav_task) {
                            if ("manager".equalsIgnoreCase(userRole)) {
                                intent = new Intent(this, ManagerTaskActivity.class);
                            } else {
                                intent = new Intent(this, TaskActivity.class);
                            }
                        } else {
                            intent = new Intent(this, EmployeeListActivity.class);
                            intent.putExtra("mode", "productivity");
                            if ("manager".equalsIgnoreCase(userRole)) {
                                intent.putExtra("role", "manager");
                            }
                        }
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (id == R.id.nav_ai_chat) {
                        showAiAssistantDialog();
                        return true;
                    }
                    return false;
                });
            }
        });
    }

    /*
     * updateCalendar:
     * - Dùng để: cập nhật lưới lịch điểm danh theo tháng.
     * - Kiểm tra/xử lý: lấy dữ liệu Attendance từ Firestore và tạo 42 ô lịch.
     * - Sau đó: đánh dấu ngày có chấm công và hiển thị chi tiết ngày được chọn.
     */
    private void updateCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(calendar.getTime()));

        calendarDays.clear();
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        tempCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

        String monthPrefix = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(calendar.getTime());
        db.collection("Attendance")
                .whereGreaterThanOrEqualTo("__name__", userId + "_" + monthPrefix + "01")
                .whereLessThanOrEqualTo("__name__", userId + "_" + monthPrefix + "31")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceMap.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String dateStr = id.substring(id.lastIndexOf("_") + 1);
                        
                        String checkIn = "";
                        String checkOut = "";
                        
                        Object ci = doc.get("checkIn");
                        if (ci instanceof com.google.firebase.Timestamp) {
                            checkIn = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(((com.google.firebase.Timestamp) ci).toDate());
                        } else if (ci instanceof String) {
                            checkIn = (String) ci;
                        }

                        Object co = doc.get("checkOut");
                        if (co instanceof com.google.firebase.Timestamp) {
                            checkOut = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(((com.google.firebase.Timestamp) co).toDate());
                        } else if (co instanceof String) {
                            checkOut = (String) co;
                        }

                        AttendanceData data = new AttendanceData(checkIn, checkOut);
                        attendanceMap.put(dateStr, data);
                    }

                    for (int i = 0; i < 42; i++) {
                        calendarDays.add(new CalendarDay((Calendar) tempCal.clone()));
                        tempCal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /*
     * CalendarDay:
     * - Dùng để: lưu một ngày trên lịch điểm danh.
     * - Kiểm tra/xử lý: giữ lại ngày và trạng thái chọn của ô lịch.
     * - Sau đó: adapter dùng dữ liệu này để vẽ grid calendar.
     */
    private class CalendarDay {
        Calendar date;
        boolean isSelected;

        CalendarDay(Calendar date) {
            this.date = date;
        }
    }

    /*
     * AttendanceData:
     * - Dùng để: lưu thời gian check-in và check-out của một ngày.
     * - Kiểm tra/xử lý: giữ chuỗi giờ vào và giờ ra sau khi đọc từ Firestore.
     * - Sau đó: màn hình hiển thị chi tiết khi bấm vào từng ngày.
     */
    private class AttendanceData {
        String checkIn, checkOut;

        AttendanceData(String checkIn, String checkOut) {
            this.checkIn = checkIn;
            this.checkOut = checkOut;
        }
    }

    /*
     * CalendarAdapter:
     * - Dùng để: hiển thị lưới 7 cột cho các ngày trong tháng.
     * - Kiểm tra/xử lý: đánh dấu ngày có điểm danh và làm mờ ngày ngoài tháng.
     * - Sau đó: người dùng nhìn lịch rõ hơn và bấm vào ngày để xem chi tiết.
     */
    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private List<CalendarDay> days;

        CalendarAdapter(List<CalendarDay> days) {
            this.days = days;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDay day = days.get(position);
            holder.tvDay.setText(String.valueOf(day.date.get(Calendar.DAY_OF_MONTH)));

            boolean isCurrentMonth = day.date.get(Calendar.MONTH) == calendar.get(Calendar.MONTH);
            holder.tvDay.setAlpha(isCurrentMonth ? 1.0f : 0.3f);

            String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(day.date.getTime());
            if (attendanceMap.containsKey(dateKey)) {
                AttendanceData data = attendanceMap.get(dateKey);
                if (data.checkIn != null && !data.checkIn.isEmpty() && data.checkOut != null && !data.checkOut.isEmpty()) {
                    holder.tvDay.setTypeface(null, Typeface.BOLD);
                    if (isManagerRole()) {
                        holder.tvDay.setTextColor(getResources().getColor(R.color.blue_dark));
                    } else {
                        holder.tvDay.setTextColor(getResources().getColor(R.color.blue_primary));
                    }
                    holder.tvDay.setBackgroundResource(R.drawable.circle_light_blue_bg);
                }
            } else {
                holder.tvDay.setTypeface(null, Typeface.NORMAL);
                holder.tvDay.setTextColor(Color.BLACK);
                holder.tvDay.setBackground(null);
            }

            holder.itemView.setOnClickListener(v -> {
                tvSelectedDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(day.date.getTime()));
                if (attendanceMap.containsKey(dateKey)) {
                    AttendanceData data = attendanceMap.get(dateKey);
                    tvDetailCheckIn.setText(data.checkIn != null && !data.checkIn.isEmpty() ? data.checkIn : "--:--");
                    tvDetailCheckOut.setText(data.checkOut != null && !data.checkOut.isEmpty() ? data.checkOut : "--:--");
                } else {
                    tvDetailCheckIn.setText("--:--");
                    tvDetailCheckOut.setText("--:--");
                }
            });
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;

            ViewHolder(View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvDay);
            }
        }
    }
}
