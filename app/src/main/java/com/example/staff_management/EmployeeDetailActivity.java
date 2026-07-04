package com.example.staff_management;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.staff_management.models.User;
import com.example.staff_management.utils.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmployeeDetailActivity extends BaseActivity {

    private ImageView ivAvatar;
    private TextView tvFullName;
    private TextView tvRolePosition;
    private View detailEmail;
    private View detailPhone;
    private View detailBirthDate;
    private View detailJoinDate;
    private View detailGender;
    private View detailDepartment;
    private View detailAddress;
    private View detailEmployeeCode;
    private View detailCitizenId;
    private View detailEmergencyName;
    private View detailEmergencyPhone;
    private View detailSalary;
    private View detailStatus;
    private MaterialButton btnDelete;
    private MaterialButton btnEdit;
    private FirebaseFirestore db;
    private String employeeId;
    private User employee;

    /*
     * onCreate:
     * - Dùng để: mở trang xem chi tiết nhân viên.
     * - Kiểm tra/xử lý: lấy employeeId từ intent rồi load dữ liệu Firestore.
     * - Sau đó: cho phép sửa hoặc xóa nhân viên từ màn hình này.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_detail);

        db = FirebaseFirestore.getInstance();
        employeeId = getIntent().getStringExtra("employeeId");
        if (employeeId == null || employeeId.isEmpty()) {
            finish();
            return;
        }

        initViews();
        loadEmployeeData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
        btnEdit.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, AddStaffActivity.class);
            intent.putExtra("editUserId", employeeId);
            startActivity(intent);
        });
    }

    /*
     * initViews:
     * - Dùng để: ánh xạ toàn bộ thông tin chi tiết của nhân viên.
     * - Kiểm tra/xử lý: gắn label và icon cho từng dòng dữ liệu.
     * - Sau đó: màn hình hiển thị profile nhìn dễ hơn.
     */
    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvRolePosition = findViewById(R.id.tvRolePosition);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);

        detailEmail = findViewById(R.id.detailEmail);
        detailPhone = findViewById(R.id.detailPhone);
        detailBirthDate = findViewById(R.id.detailBirthDate);
        detailJoinDate = findViewById(R.id.detailJoinDate);
        detailGender = findViewById(R.id.detailGender);
        detailDepartment = findViewById(R.id.detailDepartment);
        detailAddress = findViewById(R.id.detailAddress);
        detailEmployeeCode = findViewById(R.id.detailEmployeeCode);
        detailCitizenId = findViewById(R.id.detailCitizenId);
        detailEmergencyName = findViewById(R.id.detailEmergencyName);
        detailEmergencyPhone = findViewById(R.id.detailEmergencyPhone);
        detailSalary = findViewById(R.id.detailSalary);
        detailStatus = findViewById(R.id.detailStatus);

        setupDetailItem(detailEmail, R.string.detail_email_label, android.R.drawable.ic_dialog_email);
        setupDetailItem(detailPhone, R.string.detail_phone_label, android.R.drawable.ic_menu_call);
        setupDetailItem(detailBirthDate, R.string.detail_birth_date_label, android.R.drawable.ic_menu_my_calendar);
        setupDetailItem(detailJoinDate, R.string.detail_join_date_label, android.R.drawable.ic_menu_today);
        setupDetailItem(detailGender, R.string.detail_gender_label, android.R.drawable.ic_menu_info_details);
        setupDetailItem(detailDepartment, R.string.detail_department_label, android.R.drawable.ic_menu_sort_by_size);
        setupDetailItem(detailAddress, R.string.detail_address_label, android.R.drawable.ic_menu_mapmode);
        setupDetailItem(detailEmployeeCode, R.string.detail_employee_code_label, android.R.drawable.ic_menu_agenda);
        setupDetailItem(detailCitizenId, R.string.detail_citizen_id_label, android.R.drawable.ic_menu_edit);
        setupDetailItem(detailEmergencyName, R.string.detail_emergency_name_label, android.R.drawable.ic_menu_help);
        setupDetailItem(detailEmergencyPhone, R.string.detail_emergency_phone_label, android.R.drawable.ic_menu_call);
        setupDetailItem(detailSalary, R.string.detail_salary_label, android.R.drawable.ic_menu_month);
        setupDetailItem(detailStatus, R.string.detail_status_label, android.R.drawable.ic_menu_info_details);
    }

    private void setupDetailItem(View view, int labelRes, int iconRes) {
        ((TextView) view.findViewById(R.id.tvLabel)).setText(labelRes);
        ((ImageView) view.findViewById(R.id.ivIcon)).setImageResource(iconRes);
    }

    private void setDetailValue(View view, String value) {
        ((TextView) view.findViewById(R.id.tvValue)).setText(
                value != null && !value.isEmpty() ? value : getString(R.string.field_not_updated)
        );
    }

    /*
     * loadEmployeeData:
     * - Dùng để: lấy dữ liệu nhân viên từ Firestore.
     * - Kiểm tra/xử lý: đọc document Users theo employeeId.
     * - Sau đó: đổ thông tin lên avatar, tên và các trường chi tiết.
     */
    private void loadEmployeeData() {
        db.collection("Users").document(employeeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    employee = documentSnapshot.toObject(User.class);
                    if (employee == null) {
                        return;
                    }

                    tvFullName.setText(employee.getFullName());
                    tvRolePosition.setText(getString(
                            R.string.person_role_department,
                            safe(employee.getPosition()),
                            safe(employee.getDepartment())
                    ));
                    setDetailValue(detailEmail, employee.getEmail());
                    setDetailValue(detailPhone, employee.getPhoneNumber());
                    setDetailValue(detailBirthDate, employee.getBirthDate());
                    setDetailValue(detailJoinDate, employee.getJoinDate());
                    setDetailValue(detailGender, employee.getGender());
                    setDetailValue(detailDepartment, employee.getDepartment());
                    setDetailValue(detailAddress, employee.getAddress());
                    setDetailValue(detailEmployeeCode, employee.getEmployeeCode());
                    setDetailValue(detailCitizenId, employee.getCitizenId());
                    setDetailValue(detailEmergencyName, employee.getEmergencyContactName());
                    setDetailValue(detailEmergencyPhone, employee.getEmergencyContactPhone());
                    setDetailValue(detailSalary, getString(R.string.salary_vnd_format, employee.getBaseSalary()));
                    setDetailValue(detailStatus, employee.getEmploymentStatus());

                    if (employee.getAvatarUrl() != null && !employee.getAvatarUrl().isEmpty()) {
                        Glide.with(this)
                                .load(employee.getAvatarUrl())
                                .placeholder(android.R.drawable.ic_menu_report_image)
                                .into(ivAvatar);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.profile_load_error, Toast.LENGTH_SHORT).show());
    }

    /*
     * showDeleteConfirmDialog:
     * - Dùng để: mở dialog chọn kiểu xóa nhân viên.
     * - Kiểm tra/xử lý: cho chọn xóa mềm hoặc xóa hẳn.
     * - Sau đó: gọi đúng hàm xóa tương ứng.
     */
    private void showDeleteConfirmDialog() {
        String[] options = {
                getString(R.string.soft_delete_option),
                getString(R.string.hard_delete_option)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.staff_management_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        softDeleteEmployee();
                    } else {
                        showHardDeleteWarning();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /*
     * softDeleteEmployee:
     * - Dùng để: khóa nhân viên thay vì xóa hẳn.
     * - Kiểm tra/xử lý: cập nhật trạng thái nghỉ việc trên Firestore.
     * - Sau đó: nhân viên không còn hoạt động nhưng dữ liệu vẫn giữ lại.
     */
    private void softDeleteEmployee() {
        FirebaseRepository.deactivateEmployee(employeeId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.soft_delete_success, Toast.LENGTH_SHORT).show();
                    loadEmployeeData();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.error) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

    /*
     * showHardDeleteWarning:
     * - Dùng để: cảnh báo trước khi xóa vĩnh viễn.
     * - Kiểm tra/xử lý: hỏi xác nhận lại tên nhân viên.
     * - Sau đó: nếu đồng ý thì xóa toàn bộ dữ liệu liên quan.
     */
    private void showHardDeleteWarning() {
        String name = employee != null ? employee.getFullName() : getString(R.string.nav_employees);
        new AlertDialog.Builder(this)
                .setTitle(R.string.danger_delete_title)
                .setMessage(getString(R.string.danger_delete_message, name))
                .setPositiveButton(R.string.delete_forever, (dialog, which) ->
                        FirebaseRepository.hardDeleteEmployeeData(employeeId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, R.string.delete_staff_success, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        this,
                                        getString(R.string.delete_failed, e.getMessage()),
                                        Toast.LENGTH_SHORT
                                ).show()))
                .setNegativeButton(R.string.go_back, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private String safe(String value) {
        return value == null || value.isEmpty()
                ? getString(R.string.field_not_updated)
                : value;
    }
}
