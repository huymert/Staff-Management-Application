package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.example.staff_management.adapters.EmployeeAdapter;
import com.example.staff_management.models.User;
import com.example.staff_management.utils.FirebaseRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeListActivity extends BaseActivity {

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private List<User> employeeList;
    private List<User> filteredList;
    private FirebaseFirestore db;
    private String mode;
    private TextView tvTitle;
    private TextView tvTotalCount;
    private TextView tvPresentCount;
    private TextView tvAbsentCount;
    private TextView tvCurrentDate;
    private ImageButton btnBack;
    private View headerBackground;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddStaff;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;
    private final List<String> taskEmployeeIds = new ArrayList<>();
    private final Map<String, EmployeeAdapter.TaskSummary> latestTaskSummaries = new HashMap<>();
    private ListenerRegistration taskListener;
    private ListenerRegistration usersListener;

    /*
     * onCreate:
     * - Dùng để: mở màn hình danh sách nhân sự theo từng chế độ.
     * - Kiểm tra/xử lý: lấy mode và role từ intent rồi set giao diện đúng loại màn hình.
     * - Sau đó: load danh sách nhân viên, gắn tab lọc và các nút chuyển màn hình.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list);

        db = FirebaseFirestore.getInstance();
        mode = getIntent().getStringExtra("mode");
        String roleFromIntent = getIntent().getStringExtra("role");

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvTitle);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        headerBackground = findViewById(R.id.headerBackground);
        tabLayout = findViewById(R.id.tabLayout);
        fabAddStaff = findViewById(R.id.fabAddStaff);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (roleFromIntent != null) {
            applyTheme(roleFromIntent);
            if ("manager".equalsIgnoreCase(roleFromIntent)) {
                setupBottomNavigation();
            }
        }

        fabAddStaff.setVisibility("management".equals(mode) ? View.VISIBLE : View.GONE);
        fabAddStaff.setOnClickListener(v -> startActivity(new Intent(this, AddStaffActivity.class)));

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
        tvCurrentDate.setText(getString(R.string.date_placeholder) + " " + sdf.format(new java.util.Date()));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.managerEmployeesRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                android.view.ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int)(100 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                    headerContent.getPaddingLeft(),
                    systemBars.top + (int)(4 * getResources().getDisplayMetrics().density),
                    headerContent.getPaddingRight(),
                    headerContent.getPaddingBottom()
                );
                
                View dateContainer = findViewById(R.id.dateContainer);
                if (dateContainer != null) {
                    android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) dateContainer.getLayoutParams();
                    lp.topMargin = systemBars.top + (int)(58 * getResources().getDisplayMetrics().density);
                    dateContainer.setLayoutParams(lp);
                }
            }
            
            if (bottomNavigation != null) {
                bottomNavigation.setPadding(
                    bottomNavigation.getPaddingLeft(),
                    bottomNavigation.getPaddingTop(),
                    bottomNavigation.getPaddingRight(),
                    systemBars.bottom + (int)(4 * getResources().getDisplayMetrics().density)
                );
            }

            if (fabAddStaff != null) {
                android.view.ViewGroup.MarginLayoutParams fabLp = (android.view.ViewGroup.MarginLayoutParams) fabAddStaff.getLayoutParams();
                fabLp.bottomMargin = systemBars.bottom + (int)(26 * getResources().getDisplayMetrics().density);
                fabAddStaff.setLayoutParams(fabLp);
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        configureScreenByMode();

        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));

        employeeList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new EmployeeAdapter(filteredList, mode, this::onEmployeeClicked);
        rvEmployees.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                filterList(tab.getPosition());
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        fetchEmployees();
    }

    /*
     * configureScreenByMode:
     * - Dùng để: đổi tiêu đề và bố cục theo chức năng đang xem.
     * - Kiểm tra/xử lý: kiểm tra mode để biết là quản lý lương, task, quản lý nhân sự hay chấm công.
     * - Sau đó: màn hình hiển thị đúng ngữ cảnh và load dữ liệu phù hợp.
     */
    private void configureScreenByMode() {
        if ("salary".equals(mode)) {
            tvTitle.setText(R.string.manage_salaries);
        } else if ("productivity".equals(mode)) {
            tvTitle.setText(R.string.task_management);
            tabLayout.removeAllTabs();
            tabLayout.addTab(tabLayout.newTab().setText(R.string.assign_task));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.task_progress));
            observeTasks();
        } else if ("management".equals(mode)) {
            tvTitle.setText(R.string.employee_management);
        } else {
            tvTitle.setText(R.string.attendance_management);
        }
    }

    /*
     * observeTasks:
     * - Dùng để: lấy dữ liệu task theo từng nhân viên trong chế độ productivity.
     * - Kiểm tra/xử lý: nghe realtime collection Tasks và gom task mới nhất của từng người.
     * - Sau đó: cập nhật list để tab task progress lọc đúng nhân viên đã được giao việc.
     */
    private void observeTasks() {
        taskListener = db.collection("Tasks").addSnapshotListener((value, error) -> {
            if (value == null) {
                return;
            }
            taskEmployeeIds.clear();
            latestTaskSummaries.clear();
            for (QueryDocumentSnapshot doc : value) {
                String empId = doc.getString("employeeId");
                if (empId != null && !taskEmployeeIds.contains(empId)) {
                    taskEmployeeIds.add(empId);
                }
                Long timestamp = doc.getLong("timestamp");
                long safeTimestamp = timestamp != null ? timestamp : 0L;
                EmployeeAdapter.TaskSummary existing = latestTaskSummaries.get(empId);
                if (empId != null && (existing == null || safeTimestamp > existing.getTimestamp())) {
                    latestTaskSummaries.put(empId, new EmployeeAdapter.TaskSummary(
                            doc.getString("title") != null ? doc.getString("title") : "",
                            doc.getString("status"),
                            safeTimestamp
                    ));
                }
            }
            adapter.setTaskSummaries(latestTaskSummaries);
            filterList(tabLayout.getSelectedTabPosition());
        });
    }

    /*
     * fetchEmployees:
     * - Dùng để: lấy danh sách nhân viên từ Firestore.
     * - Kiểm tra/xử lý: lọc theo role hiện tại để admin, manager hay employee chỉ thấy đúng người cần xem.
     * - Sau đó: cập nhật số lượng online/offline và đổ dữ liệu lên RecyclerView.
     */
    private void fetchEmployees() {
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUid == null) {
            return;
        }

        db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(userDoc -> {
                    String currentUserRole = userDoc.getString("role");
                    String currentUserDept = userDoc.getString("department");

                    applyTheme(currentUserRole);
                    if ("manager".equalsIgnoreCase(currentUserRole)) {
                        setupBottomNavigation();
                    }

                    usersListener = db.collection("Users").addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }
                        employeeList.clear();
                        int present = 0;
                        int absent = 0;
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                User user = doc.toObject(User.class);
                                if (user == null || user.getUid() == null || user.getUid().equals(currentUid)) {
                                    continue;
                                }

                                if ("admin".equalsIgnoreCase(currentUserRole)) {
                                    if (user.getRole() != null && !"admin".equalsIgnoreCase(user.getRole())) {
                                        employeeList.add(user);
                                        if ("online".equalsIgnoreCase(user.getPresenceStatus())) {
                                            present++;
                                        } else {
                                            absent++;
                                        }
                                    }
                                } else if ("manager".equalsIgnoreCase(currentUserRole)) {
                                    if (currentUserDept != null
                                            && user.getDepartment() != null
                                            && currentUserDept.equalsIgnoreCase(user.getDepartment())
                                            && "employee".equalsIgnoreCase(user.getRole())) {
                                        employeeList.add(user);
                                        if ("online".equalsIgnoreCase(user.getPresenceStatus())) {
                                            present++;
                                        } else {
                                            absent++;
                                        }
                                    }
                                }
                            }
                        }
                        tvTotalCount.setText(String.valueOf(employeeList.size()));
                        tvPresentCount.setText(String.valueOf(present));
                        tvAbsentCount.setText(String.valueOf(absent));
                        filterList(tabLayout.getSelectedTabPosition());
                    });
                });
    }

    /*
     * applyTheme:
     * - Dùng để: đổi màu giao diện theo vai trò đang đăng nhập.
     * - Kiểm tra/xử lý: set màu cho header, tab, nút thêm và nút back.
     * - Sau đó: màn hình nhìn đúng màu chủ đạo của hệ thống.
     */
    private void applyTheme(String role) {
        int color = getResources().getColor(R.color.blue_primary);

        if (headerBackground != null) {
            headerBackground.setBackgroundColor(color);
        }
        if (tvCurrentDate != null) {
            tvCurrentDate.setTextColor(color);
        }
        if (tvTotalCount != null) {
            tvTotalCount.setTextColor(color);
        }
        if (tabLayout != null) {
            tabLayout.setSelectedTabIndicatorColor(color);
            tabLayout.setTabTextColors(getResources().getColor(R.color.black), color);
        }
        if (fabAddStaff != null) {
            fabAddStaff.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }
        if (btnBack != null && btnBack.getDrawable() != null) {
            btnBack.getDrawable().setTint(android.graphics.Color.WHITE);
        }
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho manager.
     * - Kiểm tra/xử lý: bắt sự kiện từng tab để chuyển màn hình tương ứng.
     * - Sau đó: người dùng đi qua chat, task hoặc màn hình chính nhanh hơn.
     */
    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            return;
        }
        bottomNavigation.setVisibility(View.VISIBLE);
        bottomNavigation.setSelectedItemId(R.id.nav_my_team);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_messenger) {
                startActivity(new Intent(this, UserListChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_my_team) {
                return true;
            } else if (id == R.id.nav_task) {
                startActivity(new Intent(this, ManagerTaskActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_ai_chat) {
                showAiAssistantDialog();
                return true;
            }
            return false;
        });
    }

    /*
     * onDestroy:
     * - Dùng để: dọn listener Firestore khi màn hình bị đóng.
     * - Kiểm tra/xử lý: kiểm tra listener nào đang được giữ rồi remove.
     * - Sau đó: tránh leak và tránh nghe dữ liệu nền không cần thiết.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
        if (usersListener != null) usersListener.remove();
    }

    /*
     * filterList:
     * - Dùng để: lọc danh sách nhân viên theo tab đang chọn.
     * - Kiểm tra/xử lý: tách dữ liệu theo tất cả, đang online, đang offline hoặc nhân viên có task.
     * - Sau đó: adapter nhận lại dữ liệu đã lọc và cập nhật giao diện.
     */
    private void filterList(int position) {
        filteredList.clear();
        if ("productivity".equals(mode)) {
            if (position == 0) {
                filteredList.addAll(employeeList);
            } else {
                for (User user : employeeList) {
                    if (taskEmployeeIds.contains(user.getUid())) {
                        filteredList.add(user);
                    }
                }
            }
        } else {
            if (position == 0) {
                filteredList.addAll(employeeList);
            } else if (position == 1) {
                for (User user : employeeList) {
                    if ("online".equalsIgnoreCase(user.getPresenceStatus())) {
                        filteredList.add(user);
                    }
                }
            } else if (position == 2) {
                for (User user : employeeList) {
                    if (!"online".equalsIgnoreCase(user.getPresenceStatus())) {
                        filteredList.add(user);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    /*
     * onEmployeeClicked:
     * - Dùng để: xử lý khi bấm vào một nhân viên trong danh sách.
     * - Kiểm tra/xử lý: xem đang ở chế độ quản lý, lương, task hay chấm công.
     * - Sau đó: chuyển sang màn hình chi tiết hoặc mở dialog phù hợp.
     */
    private void onEmployeeClicked(User user) {
        if ("management".equals(mode)) {
            Intent intent = new Intent(this, EmployeeDetailActivity.class);
            intent.putExtra("employeeId", user.getUid());
            startActivity(intent);
        } else if ("salary".equals(mode)) {
            showEditSalaryDialog(user);
        } else if ("productivity".equals(mode)) {
            if (tabLayout.getSelectedTabPosition() == 0) {
                showAssignTaskDialog(user);
            } else {
                showTaskOptionsDialog(user);
            }
        } else if ("attendance_view".equals(mode)) {
            showEmployeeCalendarDialog(user);
        } else {
            showDeleteConfirmDialog(user);
        }
    }

    /*
     * showEmployeeCalendarDialog:
     * - Dùng để: xem lịch chuyên cần của một nhân viên theo tháng.
     * - Kiểm tra/xử lý: lấy dữ liệu chấm công theo tháng và tính số ngày có mặt.
     * - Sau đó: hiển thị lịch, số ngày đi làm và cho phép mở báo cáo chi tiết.
     */
    private void showEmployeeCalendarDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_attendance, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvEmployeeName);
        TextView tvMonthYear = view.findViewById(R.id.tvMonthYear);
        TextView tvPresentDays = view.findViewById(R.id.tvPresentDays);
        TextView tvAbsentDays = view.findViewById(R.id.tvAbsentDays);
        RecyclerView rvCalendar = view.findViewById(R.id.rvCalendar);
        ImageButton btnPrev = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = view.findViewById(R.id.btnNextMonth);
        android.widget.Button btnClose = view.findViewById(R.id.btnClose);

        tvName.setText(user.getFullName());
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        rvCalendar.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 7));

        AlertDialog dialog = builder.create();

        Runnable updateCalendar = new Runnable() {
            @Override
            public void run() {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
                tvMonthYear.setText(sdf.format(calendar.getTime()));

                String monthPrefix = new java.text.SimpleDateFormat("yyyyMM", java.util.Locale.getDefault()).format(calendar.getTime());
                db.collection("Attendance")
                        .whereGreaterThanOrEqualTo("__name__", user.getUid() + "_" + monthPrefix + "01")
                        .whereLessThanOrEqualTo("__name__", user.getUid() + "_" + monthPrefix + "31")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            java.util.Set<Integer> presentDays = new java.util.HashSet<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                String id = doc.getId();
                                String dayStr = id.substring(id.length() - 2);
                                try {
                                    presentDays.add(Integer.parseInt(dayStr));
                                } catch (Exception ignored) {
                                }
                            }

                            List<Integer> days = new ArrayList<>();
                            java.util.Calendar tempCal = (java.util.Calendar) calendar.clone();
                            tempCal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                            int firstDayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
                            int daysInMonth = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

                            for (int i = 0; i < firstDayOfWeek; i++) {
                                days.add(null);
                            }
                            for (int i = 1; i <= daysInMonth; i++) {
                                days.add(i);
                            }

                            int totalDaysToCount;
                            java.util.Calendar today = java.util.Calendar.getInstance();
                            if (calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)
                                    && calendar.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH)) {
                                totalDaysToCount = today.get(java.util.Calendar.DAY_OF_MONTH);
                            } else if (calendar.before(today)) {
                                totalDaysToCount = daysInMonth;
                            } else {
                                totalDaysToCount = 0;
                            }

                            tvPresentDays.setText(String.valueOf(presentDays.size()));
                            tvAbsentDays.setText(String.valueOf(Math.max(0, totalDaysToCount - presentDays.size())));

                            rvCalendar.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                @androidx.annotation.NonNull
                                @Override
                                public RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                                    android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(parent.getContext());
                                    frameLayout.setLayoutParams(new android.view.ViewGroup.LayoutParams(-1, 120));

                                    TextView textView = new TextView(parent.getContext());
                                    textView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                                            (int) (40 * parent.getContext().getResources().getDisplayMetrics().density),
                                            (int) (40 * parent.getContext().getResources().getDisplayMetrics().density),
                                            android.view.Gravity.CENTER));
                                    textView.setGravity(android.view.Gravity.CENTER);
                                    frameLayout.addView(textView);

                                    return new RecyclerView.ViewHolder(frameLayout) {};
                                }

                                @Override
                                public void onBindViewHolder(@androidx.annotation.NonNull RecyclerView.ViewHolder holder, int position) {
                                    TextView tv = (TextView) ((android.widget.FrameLayout) holder.itemView).getChildAt(0);
                                    Integer day = days.get(position);
                                    if (day == null) {
                                        tv.setText("");
                                        tv.setBackground(null);
                                    } else {
                                        tv.setText(String.valueOf(day));
                                        if (presentDays.contains(day)) {
                                            tv.setTextColor(android.graphics.Color.WHITE);
                                            tv.setBackgroundResource(R.drawable.bg_present_circle);
                                            tv.setTypeface(null, android.graphics.Typeface.BOLD);
                                        } else {
                                            tv.setTextColor(android.graphics.Color.BLACK);
                                            tv.setBackground(null);
                                            tv.setTypeface(null, android.graphics.Typeface.NORMAL);

                                            if (calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)
                                                    && calendar.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH)
                                                    && day == today.get(java.util.Calendar.DAY_OF_MONTH)) {
                                                tv.setTextColor(android.graphics.Color.parseColor("#007BFF"));
                                                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public int getItemCount() {
                                    return days.size();
                                }
                            });
                        });
            }
        };

        btnPrev.setOnClickListener(v -> {
            calendar.add(java.util.Calendar.MONTH, -1);
            updateCalendar.run();
        });
        btnNext.setOnClickListener(v -> {
            calendar.add(java.util.Calendar.MONTH, 1);
            updateCalendar.run();
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        android.widget.Button btnReport = view.findViewById(R.id.btnViewReport);
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, AttendanceReportActivity.class);
                intent.putExtra("employeeId", user.getUid());
                intent.putExtra("employeeName", user.getFullName());
                startActivity(intent);
            });
        }

        updateCalendar.run();
        dialog.show();
    }

    /*
     * showTaskOptionsDialog:
     * - Dùng để: xem và hủy task gần nhất của nhân viên.
     * - Kiểm tra/xử lý: lấy task mới nhất của người đó rồi mở hộp thoại xác nhận.
     * - Sau đó: nếu đồng ý thì xóa task và báo thành công.
     */
    private void showTaskOptionsDialog(User user) {
        db.collection("Tasks")
                .whereEqualTo("employeeId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot latestDoc = queryDocumentSnapshots.getDocuments().get(0);
                        long maxTimestamp = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Long ts = doc.getLong("timestamp");
                            if (ts != null && ts > maxTimestamp) {
                                maxTimestamp = ts;
                                latestDoc = doc;
                            }
                        }

                        String taskId = latestDoc.getId();
                        String title = latestDoc.getString("title");

                        new AlertDialog.Builder(this)
                                .setTitle(getString(R.string.task_options_title, title != null ? title : ""))
                                .setMessage(getString(R.string.task_cancel_message, user.getFullName()))
                                .setPositiveButton(R.string.cancel_task, (dialog, which) ->
                                        db.collection("Tasks").document(taskId).delete()
                                                .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.task_canceled, Toast.LENGTH_SHORT).show()))
                                .setNegativeButton(R.string.close, null)
                                .show();
                    } else {
                        Toast.makeText(this, R.string.no_task_found, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /*
     * showAssignTaskDialog:
     * - Dùng để: giao task mới cho nhân viên.
     * - Kiểm tra/xử lý: nhập tiêu đề, mục tiêu, yêu cầu, mức ưu tiên và hạn hoàn thành.
     * - Sau đó: lưu task lên Firestore và gửi thông báo cho người nhận.
     */
    private void showAssignTaskDialog(User user) {
        View view = getLayoutInflater().inflate(R.layout.dialog_assign_task, null);

        EditText etTitle = view.findViewById(R.id.etTaskTitle);
        EditText etObjective = view.findViewById(R.id.etTaskObjective);
        EditText etRequirements = view.findViewById(R.id.etTaskRequirements);
        android.widget.Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        com.google.android.material.button.MaterialButton btnPickDeadline = view.findViewById(R.id.btnPickDeadline);
        TextView tvDeadlineSelected = view.findViewById(R.id.tvDeadlineSelected);

        String[] priorities = {
                getString(R.string.priority_low),
                getString(R.string.priority_medium),
                getString(R.string.priority_high)
        };
        android.widget.ArrayAdapter<String> priorityAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1);

        final long[] deadlineMillis = {0};
        btnPickDeadline.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (dp, y, m, d) -> {
                java.util.Calendar picked = java.util.Calendar.getInstance();
                picked.set(y, m, d, 23, 59, 59);
                deadlineMillis[0] = picked.getTimeInMillis();
                tvDeadlineSelected.setText(getString(R.string.deadline_selected, d, m + 1, y));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String objective = etObjective.getText().toString().trim();
            String requirements = etRequirements.getText().toString().trim();
            String priority = priorities[spinnerPriority.getSelectedItemPosition()];

            if (title.isEmpty()) {
                Toast.makeText(this, R.string.task_title_required, Toast.LENGTH_SHORT).show();
                return;
            }

            String taskId = db.collection("Tasks").document().getId();
            com.example.staff_management.models.Task task = new com.example.staff_management.models.Task(
                    taskId,
                    title,
                    objective,
                    requirements,
                    user.getUid(),
                    user.getFullName(),
                    com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                    "Pending",
                    System.currentTimeMillis()
            );
            task.setPriority(priority);
            task.setDeadline(deadlineMillis[0]);

            FirebaseRepository.tasks().document(taskId).set(task)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.task_assigned_success, user.getFullName()), Toast.LENGTH_SHORT).show();
                        com.example.staff_management.utils.NotificationHelper.sendToUser(
                                user.getUid(),
                                getString(R.string.new_task_notification),
                                title
                        );
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, R.string.task_assigned_failed, Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    /*
     * showDeleteConfirmDialog:
     * - Dùng để: xác nhận xóa nhân viên khỏi hệ thống.
     * - Kiểm tra/xử lý: mở dialog hỏi lại trước khi xóa document.
     * - Sau đó: nếu đồng ý thì xóa dữ liệu nhân viên trên Firestore.
     */
    private void showDeleteConfirmDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_staff_title)
                .setMessage(getString(R.string.delete_staff_message, user.getFullName()))
                .setPositiveButton(R.string.delete_staff_title, (dialog, which) ->
                        db.collection("Users").document(user.getUid()).delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.delete_staff_success, Toast.LENGTH_SHORT).show()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /*
     * showEditSalaryDialog:
     * - Dùng để: cập nhật lương cơ bản cho nhân viên.
     * - Kiểm tra/xử lý: nhập số lương mới và kiểm tra định dạng số.
     * - Sau đó: lưu dữ liệu lương mới lên Firestore.
     */
    private void showEditSalaryDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.update_salary_title, user.getFullName()));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(user.getBaseSalary()));
        builder.setView(input);

        builder.setPositiveButton(R.string.update_staff_info, (dialog, which) -> {
            String salaryInput = input.getText().toString().trim();
            if (salaryInput.isEmpty()) {
                Toast.makeText(this, R.string.salary_required, Toast.LENGTH_SHORT).show();
                return;
            }

            double newSalary;
            try {
                newSalary = Double.parseDouble(salaryInput);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.invalid_salary_format, Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(user.getUid())
                    .update("baseSalary", newSalary)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.salary_updated, Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
