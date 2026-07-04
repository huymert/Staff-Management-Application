package com.example.staff_management;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.staff_management.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddStaffActivity extends BaseActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etBaseSalary;
    private TextInputEditText etBirthDate;
    private TextInputEditText etJoinDate;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etAddress;
    private TextInputEditText etEmployeeCode;
    private TextInputEditText etCitizenId;
    private TextInputEditText etEmergencyContactName;
    private TextInputEditText etEmergencyContactPhone;
    private RadioGroup rgGender;
    private AutoCompleteTextView autoCompleteDepartment;
    private AutoCompleteTextView autoCompletePosition;
    private AutoCompleteTextView autoCompleteStatus;
    private Button btnAddStaff;
    private ImageButton btnBack;
    private TextView tvTitle;
    private FirebaseFirestore db;
    private String editUserId;
    private boolean isEditMode;

    /*
     * onCreate:
     * - Dùng để: mở màn hình thêm hoặc sửa nhân viên.
     * - Kiểm tra/xử lý: lấy dữ liệu từ intent để biết đang ở chế độ thêm hay sửa.
     * - Sau đó: load form, dropdown và nút lưu dữ liệu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupDropdowns();
        setupDatePickers();

        editUserId = getIntent().getStringExtra("editUserId");
        if (editUserId != null) {
            isEditMode = true;
            tvTitle.setText(R.string.edit_staff_title);
            btnAddStaff.setText(R.string.update_staff_info);
            findViewById(R.id.tilPassword).setVisibility(View.GONE);
            loadStaffDataForEdit();
        }

        btnBack.setOnClickListener(v -> finish());
        btnAddStaff.setOnClickListener(v -> validateAndProcess());
    }

    /*
     * initViews:
     * - Dùng để: ánh xạ các ô nhập liệu của form nhân viên.
     * - Kiểm tra/xử lý: gom hết view quan trọng ra biến để dùng lại.
     * - Sau đó: các hàm validate và lưu dữ liệu có thể chạy tiếp.
     */
    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etBaseSalary = findViewById(R.id.etBaseSalary);
        etBirthDate = findViewById(R.id.etBirthDate);
        etJoinDate = findViewById(R.id.etJoinDate);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAddress = findViewById(R.id.etAddress);
        etEmployeeCode = findViewById(R.id.etEmployeeCode);
        etCitizenId = findViewById(R.id.etCitizenId);
        etEmergencyContactName = findViewById(R.id.etEmergencyContactName);
        etEmergencyContactPhone = findViewById(R.id.etEmergencyContactPhone);
        rgGender = findViewById(R.id.rgGender);
        autoCompleteDepartment = findViewById(R.id.autoCompleteDepartment);
        autoCompletePosition = findViewById(R.id.autoCompletePosition);
        autoCompleteStatus = findViewById(R.id.autoCompleteStatus);
        btnAddStaff = findViewById(R.id.btnAddStaff);
        btnBack = findViewById(R.id.btnBack);
    }

    /*
     * setupDropdowns:
     * - Dùng để: set danh sách chọn cho phòng ban, chức vụ và trạng thái.
     * - Kiểm tra/xử lý: lấy text từ resources để hiện đúng ngôn ngữ.
     * - Sau đó: người dùng chọn nhanh hơn và ít nhập sai hơn.
     */
    private void setupDropdowns() {
        String[] departments = {
                getString(R.string.department_it),
                getString(R.string.department_sales),
                getString(R.string.department_marketing),
                getString(R.string.department_hr),
                getString(R.string.department_finance)
        };
        autoCompleteDepartment.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, departments));

        String[] positions = {
                getString(R.string.position_employee),
                getString(R.string.position_manager)
        };
        autoCompletePosition.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, positions));

        String[] statuses = {
                getString(R.string.employment_active),
                getString(R.string.employment_probation),
                getString(R.string.employment_resigned)
        };
        autoCompleteStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses));
    }

    /*
     * setupDatePickers:
     * - Dùng để: mở date picker cho ngày sinh và ngày vào làm.
     * - Kiểm tra/xử lý: khi bấm vào ô thì bật dialog chọn ngày.
     * - Sau đó: dữ liệu ngày được nhập theo format thống nhất.
     */
    private void setupDatePickers() {
        etBirthDate.setOnClickListener(v -> showDatePicker(etBirthDate));
        etJoinDate.setOnClickListener(v -> showDatePicker(etJoinDate));
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            editText.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /*
     * loadStaffDataForEdit:
     * - Dùng để: lấy dữ liệu nhân viên cũ khi ở chế độ sửa.
     * - Kiểm tra/xử lý: đọc document users theo edituserid.
     * - Sau đó: đổ dữ liệu lên form để chỉnh sửa lại.
     */
    private void loadStaffDataForEdit() {
        db.collection("Users").document(editUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        return;
                    }

                    etFullName.setText(user.getFullName());
                    etEmail.setText(user.getEmail());
                    etEmail.setEnabled(false);
                    etBaseSalary.setText(String.valueOf((long) user.getBaseSalary()));
                    etBirthDate.setText(user.getBirthDate());
                    etJoinDate.setText(user.getJoinDate());
                    etPhoneNumber.setText(user.getPhoneNumber());
                    etAddress.setText(user.getAddress());
                    etEmployeeCode.setText(user.getEmployeeCode());
                    etCitizenId.setText(user.getCitizenId());
                    etEmergencyContactName.setText(user.getEmergencyContactName());
                    etEmergencyContactPhone.setText(user.getEmergencyContactPhone());
                    autoCompleteDepartment.setText(user.getDepartment(), false);
                    autoCompletePosition.setText(user.getPosition(), false);
                    autoCompleteStatus.setText(user.getEmploymentStatus(), false);

                    if (getString(R.string.female).equalsIgnoreCase(user.getGender())) {
                        rgGender.check(R.id.rbFemale);
                    } else {
                        rgGender.check(R.id.rbMale);
                    }
                });
    }

    /*
     * validateAndProcess:
     * - Dùng để: chọn luồng thêm mới hoặc cập nhật nhân viên.
     * - Kiểm tra/xử lý: xem có đang ở edit mode hay không.
     * - Sau đó: gọi đúng hàm xử lý dữ liệu tương ứng.
     */
    private void validateAndProcess() {
        if (isEditMode) {
            validateAndUpdate();
        } else {
            validateAndCreateAccount();
        }
    }

    private void validateAndUpdate() {
        FormData form = collectFormData(false);
        if (form == null) {
            return;
        }
        updateStaffData(form);
    }

    private void validateAndCreateAccount() {
        FormData form = collectFormData(true);
        if (form == null) {
            return;
        }
        createStaffAccount(form);
    }

    private FormData collectFormData(boolean requireCredentials) {
        FormData data = new FormData();
        data.fullName = valueOf(etFullName);
        data.birthDate = valueOf(etBirthDate);
        data.joinDate = valueOf(etJoinDate);
        data.phone = valueOf(etPhoneNumber);
        data.email = valueOf(etEmail);
        data.password = valueOf(etPassword);
        data.department = autoCompleteDepartment.getText().toString().trim();
        data.position = autoCompletePosition.getText().toString().trim();
        data.salaryRaw = valueOf(etBaseSalary);
        data.status = autoCompleteStatus.getText().toString().trim();
        data.address = valueOf(etAddress);
        data.employeeCode = valueOf(etEmployeeCode);
        data.citizenId = valueOf(etCitizenId);
        data.emergencyContactName = valueOf(etEmergencyContactName);
        data.emergencyContactPhone = valueOf(etEmergencyContactPhone);

        if (!validateCommonFields(data)) {
            return null;
        }

        if (requireCredentials) {
            if (!Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) {
                etEmail.setError(getString(R.string.validation_invalid_email));
                return null;
            }
            if (data.password.length() < 6) {
                etPassword.setError(getString(R.string.validation_password_length));
                return null;
            }
        }

        Double parsedSalary = parseSalary(data.salaryRaw);
        if (parsedSalary == null) {
            etBaseSalary.setError(getString(R.string.validation_invalid_salary));
            return null;
        }
        data.salary = parsedSalary;

        data.gender = ((RadioButton) findViewById(rgGender.getCheckedRadioButtonId())).getText().toString();
        data.role = resolveRole(data.position);
        return data;
    }

    private boolean validateCommonFields(FormData data) {
        if (data.fullName.split("\\s+").length < 2) {
            etFullName.setError(getString(R.string.validation_name_two_words));
            return false;
        }
        if (TextUtils.isEmpty(data.birthDate)) {
            Toast.makeText(this, R.string.validation_select_birth_date, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(data.joinDate)) {
            Toast.makeText(this, R.string.validation_select_join_date, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!data.phone.matches("^0\\d{9}$")) {
            etPhoneNumber.setError(getString(R.string.validation_invalid_phone));
            return false;
        }
        if (TextUtils.isEmpty(data.department) || TextUtils.isEmpty(data.position) || TextUtils.isEmpty(data.status)) {
            Toast.makeText(this, R.string.validation_missing_required_fields, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(data.salaryRaw)) {
            etBaseSalary.setError(getString(R.string.validation_base_salary_required));
            return false;
        }
        if (TextUtils.isEmpty(data.employeeCode)) {
            etEmployeeCode.setError(getString(R.string.validation_employee_code_required));
            return false;
        }
        if (TextUtils.isEmpty(data.citizenId)) {
            etCitizenId.setError(getString(R.string.validation_citizen_id_required));
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, R.string.validation_gender_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!TextUtils.isEmpty(data.emergencyContactPhone) && !data.emergencyContactPhone.matches("^0\\d{9}$")) {
            etEmergencyContactPhone.setError(getString(R.string.validation_emergency_phone_invalid));
            return false;
        }
        return true;
    }

    /*
     * updateStaffData:
     * - Dùng để: lưu lại dữ liệu nhân viên khi sửa.
     * - Kiểm tra/xử lý: update document Firestore theo form đã nhập.
     * - Sau đó: đóng màn hình và báo sửa thành công.
     */
    private void updateStaffData(FormData form) {
        db.collection("Users").document(editUserId)
                .update(
                        "fullName", form.fullName,
                        "birthDate", form.birthDate,
                        "joinDate", form.joinDate,
                        "phoneNumber", form.phone,
                        "department", form.department,
                        "position", form.position,
                        "baseSalary", form.salary,
                        "status", form.status,
                        "employmentStatus", form.status,
                        "presenceStatus", "offline",
                        "gender", form.gender,
                        "role", form.role,
                        "address", form.address,
                        "employeeCode", form.employeeCode,
                        "citizenId", form.citizenId,
                        "emergencyContactName", form.emergencyContactName,
                        "emergencyContactPhone", form.emergencyContactPhone
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.update_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.update_failed, e.getMessage()),
                        Toast.LENGTH_SHORT
                ).show());
    }

    /*
     * createStaffAccount:
     * - Dùng để: tạo tài khoản nhân viên mới bằng Firebase Auth.
     * - Kiểm tra/xử lý: tạo user auth tạm, rồi lưu tiếp profile vào Firestore.
     * - Sau đó: user mới xuất hiện trong collection Users của hệ thống.
     */
    private void createStaffAccount(FormData form) {
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.initializeApp(this, options, "staffCreator");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.getInstance("staffCreator");
        }
        final FirebaseApp appForCreation = secondaryApp;
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(appForCreation);

        secondaryAuth.createUserWithEmailAndPassword(form.email, form.password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                        secondaryAuth.signOut();
                        appForCreation.delete();
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.create_account_failed);
                        Toast.makeText(this, getString(R.string.register_failed, message), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    User newUser = new User(uid, form.fullName, form.email, form.role, form.status, form.salary);
                    newUser.setEmploymentStatus(form.status);
                    newUser.setPresenceStatus("offline");
                    newUser.setBirthDate(form.birthDate);
                    newUser.setGender(form.gender);
                    newUser.setPhoneNumber(form.phone);
                    newUser.setAddress(form.address);
                    newUser.setDepartment(form.department);
                    newUser.setPosition(form.position);
                    newUser.setJoinDate(form.joinDate);
                    newUser.setEmployeeCode(form.employeeCode);
                    newUser.setCitizenId(form.citizenId);
                    newUser.setEmergencyContactName(form.emergencyContactName);
                    newUser.setEmergencyContactPhone(form.emergencyContactPhone);

                    db.collection("Users").document(uid).set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                secondaryAuth.signOut();
                                appForCreation.delete();
                                Toast.makeText(this, R.string.staff_created_success, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                secondaryAuth.signOut();
                                appForCreation.delete();
                                Toast.makeText(
                                        this,
                                        getString(R.string.firestore_error, e.getMessage()),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                });
    }

    private String resolveRole(String position) {
        String lower = position.toLowerCase(Locale.ROOT);
        if (lower.contains("admin") || lower.contains("giám đốc") || lower.contains("giam doc")) {
            return "admin";
        }
        if (lower.contains("manager") || lower.contains("trưởng phòng") || lower.contains("truong phong")) {
            return "manager";
        }
        return "employee";
    }

    private Double parseSalary(String salaryStr) {
        try {
            double salary = Double.parseDouble(salaryStr);
            return salary >= 0 ? salary : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static class FormData {
        private String fullName;
        private String birthDate;
        private String joinDate;
        private String phone;
        private String email;
        private String password;
        private String department;
        private String position;
        private String salaryRaw;
        private String status;
        private String address;
        private String employeeCode;
        private String citizenId;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String gender;
        private String role;
        private double salary;
    }
}
