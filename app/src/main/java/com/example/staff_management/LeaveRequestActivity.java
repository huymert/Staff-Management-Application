package com.example.staff_management;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.adapters.LeaveRequestAdapter;
import com.example.staff_management.models.LeaveRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * LeaveRequestActivity:
 * - Dùng để: mở màn hình quản lý đơn xin nghỉ phép.
 * - Kiểm tra/xử lý: phân quyền employee, manager và admin theo dữ liệu Firestore.
 * - Sau đó: hiển thị đúng danh sách đơn và cho phép tạo hoặc duyệt đơn.
 */
public class LeaveRequestActivity extends BaseActivity {

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvTotalRequests;
    private TextView tvPendingRequests;
    private TextView tvApprovedRequests;
    private TextView tvEmptyState;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddLeave;
    private RecyclerView rvLeaveRequests;

    private final List<LeaveRequest> allRequests = new ArrayList<>();
    private final List<LeaveRequest> filteredRequests = new ArrayList<>();
    private LeaveRequestAdapter adapter;

    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserRole = "employee";
    private String currentUserDepartment = "";
    private String currentUserName = "";
    private ListenerRegistration leaveRequestListener;

    /*
     * onCreate:
     * - Dùng để: khởi tạo màn hình đơn nghỉ phép.
     * - Kiểm tra/xử lý: lấy user hiện tại, load role rồi cấu hình UI theo quyền.
     * - Sau đó: bắt đầu tải danh sách đơn và hiển thị tab lọc.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_request);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        initViews();
        setupList();
        setupTabs();
        loadCurrentUserAndRequests();
    }

    /*
     * initViews:
     * - Dùng để: ánh xạ các view trên màn hình.
     * - Kiểm tra/xử lý: set safe area cho header, FAB và nút back.
     * - Sau đó: chuẩn bị giao diện trước khi load dữ liệu.
     */
    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leaveRequestsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int)(140 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                    headerContent.getPaddingLeft(),
                    systemBars.top + (int)(8 * getResources().getDisplayMetrics().density),
                    headerContent.getPaddingRight(),
                    headerContent.getPaddingBottom()
                );
            }
            
            if (fabAddLeave != null) {
                ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fabAddLeave.getLayoutParams();
                fabLp.bottomMargin = systemBars.bottom + (int)(26 * getResources().getDisplayMetrics().density);
                fabAddLeave.setLayoutParams(fabLp);
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvApprovedRequests = findViewById(R.id.tvApprovedRequests);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tabLayout = findViewById(R.id.tabLayoutLeave);
        fabAddLeave = findViewById(R.id.fabAddLeave);
        rvLeaveRequests = findViewById(R.id.rvLeaveRequests);
    }

    /*
     * setupList:
     * - Dùng để: set RecyclerView cho danh sách đơn nghỉ.
     * - Kiểm tra/xử lý: gắn adapter có nút duyệt và từ chối.
     * - Sau đó: mỗi item sẽ phản hồi đúng theo quyền người xem.
     */
    private void setupList() {
        adapter = new LeaveRequestAdapter(filteredRequests, false, new LeaveRequestAdapter.LeaveActionListener() {
            @Override
            public void onApprove(LeaveRequest request) {
                approveLeaveRequest(request);
            }

            @Override
            public void onReject(LeaveRequest request) {
                showRejectDialog(request);
            }
        });
        rvLeaveRequests.setLayoutManager(new LinearLayoutManager(this));
        rvLeaveRequests.setAdapter(adapter);
    }

    /*
     * setupTabs:
     * - Dùng để: tạo tab lọc đơn đang chờ và lịch sử.
     * - Kiểm tra/xử lý: nghe sự kiện chọn tab để lọc lại dữ liệu.
     * - Sau đó: list chỉ hiện phần dữ liệu đúng tab.
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.pending_requests));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.request_history));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyCurrentFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /*
     * loadCurrentUserAndRequests:
     * - Dùng để: lấy role và phòng ban của người dùng hiện tại.
     * - Kiểm tra/xử lý: đọc Firestore rồi quyết định có quyền duyệt hay không.
     * - Sau đó: gắn adapter đúng quyền và bắt đầu lắng nghe realtime.
     */
    private void loadCurrentUserAndRequests() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentUserRole = safe(snapshot.getString("role"), "employee");
                        currentUserDepartment = safe(snapshot.getString("department"), "");
                        currentUserName = safe(snapshot.getString("fullName"), "");
                    }
                    boolean canReview = isReviewer();
                    adapter = new LeaveRequestAdapter(filteredRequests, canReview, new LeaveRequestAdapter.LeaveActionListener() {
                        @Override
                        public void onApprove(LeaveRequest request) {
                            approveLeaveRequest(request);
                        }

                        @Override
                        public void onReject(LeaveRequest request) {
                            showRejectDialog(request);
                        }
                    });
                    rvLeaveRequests.setAdapter(adapter);
                    configureUiForRole();
                    listenLeaveRequests();
                })
                .addOnFailureListener(e -> {
                    configureUiForRole();
                    listenLeaveRequests();
                });
    }

    /*
     * configureUiForRole:
     * - Dùng để: chỉnh giao diện theo quyền người dùng.
     * - Kiểm tra/xử lý: ẩn nút tạo đơn với manager/admin, hiện với employee.
     * - Sau đó: tiêu đề và empty state sẽ đổi theo đúng vai trò.
     */
    private void configureUiForRole() {
        boolean canReview = isReviewer();
        tvTitle.setText(R.string.leave_requests_title);
        tvSubtitle.setText(canReview ? R.string.leave_manage_subtitle : R.string.leave_my_subtitle);
        fabAddLeave.setVisibility(canReview ? View.GONE : View.VISIBLE);
        fabAddLeave.setOnClickListener(v -> showCreateLeaveDialog());
        tvEmptyState.setText(canReview ? R.string.leave_empty_state_manager : R.string.leave_empty_state_employee);
    }

    /*
     * listenLeaveRequests:
     * - Dùng để: nghe realtime thay đổi của collection LeaveRequests.
     * - Kiểm tra/xử lý: lọc đơn phù hợp, sắp xếp và cập nhật tổng quan.
     * - Sau đó: giao diện tự refresh khi dữ liệu trên Firestore đổi.
     */
    private void listenLeaveRequests() {
        leaveRequestListener = db.collection("LeaveRequests")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, getString(R.string.leave_action_failed, error.getMessage()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    allRequests.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            LeaveRequest request = doc.toObject(LeaveRequest.class);
                            request.setId(doc.getId());
                            if (shouldIncludeRequest(request)) {
                                allRequests.add(request);
                            }
                        }
                    }
                    sortRequests();
                    updateSummary();
                    applyCurrentFilter();
                });
    }

    /*
     * onDestroy:
     * - Dùng để: dọn listener khi màn hình đóng.
     * - Kiểm tra/xử lý: remove snapshot listener của đơn nghỉ.
     * - Sau đó: tránh nghe realtime sai chỗ và giảm rò rỉ bộ nhớ.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaveRequestListener != null) leaveRequestListener.remove();
    }

    /*
     * shouldIncludeRequest:
     * - Dùng để: kiểm tra đơn nào được phép hiện lên.
     * - Kiểm tra/xử lý: so quyền admin, manager và employee theo phòng ban hoặc userId.
     * - Sau đó: chỉ những đơn đúng phạm vi mới được đưa vào list.
     */
    private boolean shouldIncludeRequest(LeaveRequest request) {
        if (request == null) {
            return false;
        }
        if (isReviewer()) {
            if ("admin".equalsIgnoreCase(currentUserRole)) {
                return true;
            }
            return request.getDepartment() != null
                    && request.getDepartment().equalsIgnoreCase(currentUserDepartment);
        }
        return currentUserId.equals(request.getUserId());
    }

    /*
     * sortRequests:
     * - Dùng để: sắp xếp đơn nghỉ theo thời gian tạo.
     * - Kiểm tra/xử lý: đưa đơn mới nhất lên đầu danh sách.
     * - Sau đó: người dùng dễ xem đơn gần nhất hơn.
     */
    private void sortRequests() {
        allRequests.sort((a, b) -> {
            Date aDate = a.getCreatedAt();
            Date bDate = b.getCreatedAt();
            long aMillis = aDate != null ? aDate.getTime() : 0L;
            long bMillis = bDate != null ? bDate.getTime() : 0L;
            return Long.compare(bMillis, aMillis);
        });
    }

    /*
     * updateSummary:
     * - Dùng để: cập nhật bảng tóm tắt đơn nghỉ.
     * - Kiểm tra/xử lý: đếm tổng số đơn, đơn đang chờ và đơn đã duyệt.
     * - Sau đó: các ô thống kê trên đầu màn hình đổi theo dữ liệu mới.
     */
    private void updateSummary() {
        int total = allRequests.size();
        int pending = 0;
        int approved = 0;
        for (LeaveRequest request : allRequests) {
            if ("approved".equalsIgnoreCase(request.getStatus())) {
                approved++;
            }
            if ("pending".equalsIgnoreCase(request.getStatus())) {
                pending++;
            }
        }
        tvTotalRequests.setText(String.valueOf(total));
        tvPendingRequests.setText(String.valueOf(pending));
        tvApprovedRequests.setText(String.valueOf(approved));
    }

    /*
     * applyCurrentFilter:
     * - Dùng để: lọc danh sách đơn theo tab đang chọn.
     * - Kiểm tra/xử lý: tách đơn pending và đơn lịch sử theo trạng thái.
     * - Sau đó: RecyclerView chỉ hiện đúng nhóm đơn người dùng cần xem.
     */
    private void applyCurrentFilter() {
        boolean showPending = tabLayout.getSelectedTabPosition() == 0;
        filteredRequests.clear();
        for (LeaveRequest request : allRequests) {
            boolean pending = "pending".equalsIgnoreCase(request.getStatus());
            if ((showPending && pending) || (!showPending && !pending)) {
                filteredRequests.add(request);
            }
        }
        adapter.notifyDataSetChanged();
        boolean hasItems = !filteredRequests.isEmpty();
        tvEmptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        rvLeaveRequests.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    }

    /*
     * isReviewer:
     * - Dùng để: kiểm tra người dùng hiện tại có quyền duyệt đơn không.
     * - Kiểm tra/xử lý: so role có phải manager hoặc admin hay không.
     * - Sau đó: màn hình biết có hiện nút duyệt hay không.
     */
    private boolean isReviewer() {
        return "manager".equalsIgnoreCase(currentUserRole) || "admin".equalsIgnoreCase(currentUserRole);
    }

    /*
     * showCreateLeaveDialog:
     * - Dùng để: mở dialog tạo đơn xin nghỉ mới.
     * - Kiểm tra/xử lý: chọn loại nghỉ, ngày bắt đầu, ngày kết thúc và lý do.
     * - Sau đó: gọi luồng lưu đơn nếu dữ liệu hợp lệ.
     */
    private void showCreateLeaveDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_leave_request, null);

        final android.widget.Spinner spinner = view.findViewById(R.id.spinnerLeaveType);
        final EditText etStartDate = view.findViewById(R.id.etStartDate);
        final EditText etEndDate = view.findViewById(R.id.etEndDate);
        final EditText etReason = view.findViewById(R.id.etReason);

        final String[] leaveTypes = {
                getString(R.string.leave_type_annual),
                getString(R.string.leave_type_sick),
                getString(R.string.leave_type_unpaid),
                getString(R.string.leave_type_remote)
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, leaveTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        final Calendar[] startCalendar = {null};
        final Calendar[] endCalendar = {null};

        etStartDate.setOnClickListener(v -> showDatePicker(selected -> {
            startCalendar[0] = selected;
            etStartDate.setText(formatDate(selected.getTime()));
        }));

        etEndDate.setOnClickListener(v -> showDatePicker(selected -> {
            endCalendar[0] = selected;
            etEndDate.setText(formatDate(selected.getTime()));
        }));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (startCalendar[0] == null || endCalendar[0] == null) {
                Toast.makeText(this, R.string.validation_leave_dates, Toast.LENGTH_SHORT).show();
                return;
            }
            if (endCalendar[0].before(startCalendar[0])) {
                Toast.makeText(this, R.string.validation_leave_date_order, Toast.LENGTH_SHORT).show();
                return;
            }
            if (reason.isEmpty()) {
                Toast.makeText(this, R.string.validation_leave_reason, Toast.LENGTH_SHORT).show();
                return;
            }
            createLeaveRequest(
                    leaveTypes[spinner.getSelectedItemPosition()],
                    reason,
                    startCalendar[0].getTime(),
                    endCalendar[0].getTime(),
                    dialog
            );
        });

        dialog.show();
    }

    /*
     * DateCallback:
     * - Dùng để: nhận ngày người dùng chọn từ DatePickerDialog.
     * - Kiểm tra/xử lý: trả về Calendar đã được chọn.
     * - Sau đó: dialog tạo đơn lấy ngày này để điền lên form.
     */
    private interface DateCallback {
        void onDateSelected(Calendar selected);
    }

    /*
     * showDatePicker:
     * - Dùng để: mở lịch cho user chọn ngày.
     * - Kiểm tra/xử lý: lấy ngày hiện tại làm mốc rồi trả về ngày đã chọn.
     * - Sau đó: form tạo đơn được điền ngày đúng format.
     */
    private void showDatePicker(DateCallback callback) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            callback.onDateSelected(selected);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    /*
     * createLeaveRequest:
     * - Dùng để: tạo và lưu đơn xin nghỉ mới.
     * - Kiểm tra/xử lý: tính số ngày nghỉ và gom dữ liệu form vào map.
     * - Sau đó: ghi document mới lên Firestore với trạng thái pending.
     */
    private void createLeaveRequest(String leaveType, String reason, Date startDate, Date endDate, AlertDialog dialog) {
        String requestId = db.collection("LeaveRequests").document().getId();
        long totalDays = ((endDate.getTime() - startDate.getTime()) / (24L * 60L * 60L * 1000L)) + 1;

        Map<String, Object> data = new HashMap<>();
        data.put("id", requestId);
        data.put("userId", currentUserId);
        data.put("userName", currentUserName);
        data.put("department", currentUserDepartment);
        data.put("leaveType", leaveType);
        data.put("reason", reason);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("totalDays", totalDays);
        data.put("status", "pending");
        data.put("createdAt", new Date());
        data.put("updatedAt", new Date());

        db.collection("LeaveRequests").document(requestId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.submit_leave_success, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.submit_leave_failed, e.getMessage()),
                        Toast.LENGTH_SHORT
                ).show());
    }

    /*
     * approveLeaveRequest:
     * - Dùng để: duyệt nhanh một đơn xin nghỉ.
     * - Kiểm tra/xử lý: gọi hàm cập nhật trạng thái sang approved.
     * - Sau đó: đơn được ghi nhận là đã duyệt trên Firestore.
     */
    private void approveLeaveRequest(LeaveRequest request) {
        updateLeaveRequestStatus(request, "approved", null);
    }

    /*
     * showRejectDialog:
     * - Dùng để: mở dialog nhập lý do từ chối đơn nghỉ.
     * - Kiểm tra/xử lý: người duyệt nhập lý do trước khi reject.
     * - Sau đó: gọi hàm cập nhật trạng thái kèm lý do từ chối.
     */
    private void showRejectDialog(LeaveRequest request) {
        final EditText input = new EditText(this);
        input.setHint(R.string.rejection_reason_hint);
        input.setMinLines(3);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);

        new AlertDialog.Builder(this)
                .setTitle(R.string.rejection_reason_title)
                .setView(input)
                .setPositiveButton(R.string.reject, (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    updateLeaveRequestStatus(request, "rejected", reason);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /*
     * updateLeaveRequestStatus:
     * - Dùng để: cập nhật trạng thái đơn nghỉ trên Firestore.
     * - Kiểm tra/xử lý: lưu trạng thái approved hoặc rejected kèm người duyệt.
     * - Sau đó: đơn được đồng bộ lại và hiện thông báo kết quả.
     */
    private void updateLeaveRequestStatus(LeaveRequest request, String status, String rejectionReason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", new Date());
        updates.put("approverId", currentUserId);
        updates.put("approverName", currentUserName);
        updates.put("rejectionReason", rejectionReason);

        db.collection("LeaveRequests").document(request.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(
                        this,
                        "approved".equals(status) ? R.string.leave_approve_success : R.string.leave_reject_success,
                        Toast.LENGTH_SHORT
                ).show())
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.leave_action_failed, e.getMessage()),
                        Toast.LENGTH_SHORT
                ).show());
    }

    /*
     * formatDate:
     * - Dùng để: đổi Date sang chuỗi dd/MM/yyyy.
     * - Kiểm tra/xử lý: dùng SimpleDateFormat theo locale hiện tại.
     * - Sau đó: ngày hiển thị dễ đọc trong form và dialog.
     */
    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }

    /*
     * safe:
     * - Dùng để: trả về giá trị mặc định khi chuỗi bị null hoặc rỗng.
     * - Kiểm tra/xử lý: so sánh giá trị đầu vào với null và chuỗi trống.
     * - Sau đó: tránh lỗi khi hiển thị dữ liệu thiếu.
     */
    private String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
