package com.example.staff_management;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.models.User;
import com.example.staff_management.utils.FirebaseRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalaryCalculationActivity extends BaseActivity {

    private static final int WORKING_DAYS = 26;
    private static final double MAX_PRODUCTIVITY_BONUS = 0.2;

    private TextView tvMonthYear;
    private RecyclerView rvSalary;
    private FirebaseFirestore db;
    private Calendar calendar;
    private SalaryAdapter adapter;
    private List<SalaryRecord> salaryList = new ArrayList<>();
    private NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /*
     * onCreate:
     * - Dùng để: mở màn hình tính lương và chuẩn bị danh sách hiển thị.
     * - Kiểm tra/xử lý: gắn RecyclerView, nút chuyển tháng và safe area cho header.
     * - Sau đó: gọi loadSalaries để lấy dữ liệu từ Firestore.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_calculation);

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        tvMonthYear = findViewById(R.id.tvMonthYear);
        rvSalary = findViewById(R.id.rvSalary);

        adapter = new SalaryAdapter(salaryList);
        rvSalary.setLayoutManager(new LinearLayoutManager(this));
        rvSalary.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> { calendar.add(Calendar.MONTH, -1); loadSalaries(); });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> { calendar.add(Calendar.MONTH, 1); loadSalaries(); });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.salaryCalculationRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int) (80 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                        headerContent.getPaddingLeft(),
                        systemBars.top + (int) (8 * getResources().getDisplayMetrics().density),
                        headerContent.getPaddingRight(),
                        headerContent.getPaddingBottom()
                );
            }

            return WindowInsetsCompat.CONSUMED;
        });

        loadSalaries();
    }

    /*
     * loadSalaries:
     * - Dùng để: lấy danh sách nhân viên và dữ liệu chấm công trong tháng.
     * - Kiểm tra/xử lý: đếm số ngày đi làm rồi ghép với điểm năng suất để ra lương thực nhận.
     * - Sau đó: đổ dữ liệu đã tính vào RecyclerView.
     */
    private void loadSalaries() {
        tvMonthYear.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        String monthPrefix = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(calendar.getTime());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;

        db.collection("Users")
                .whereNotEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(userSnap -> {
                    List<User> employees = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : userSnap) {
                        User u = doc.toObject(User.class);
                        if (u != null && u.getUid() != null && !"admin".equalsIgnoreCase(u.getRole())) {
                            u.setUid(doc.getId());
                            employees.add(u);
                        }
                    }

                    salaryList.clear();
                    if (employees.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    FirebaseRepository.attendance()
                            .whereEqualTo("month", month)
                            .whereEqualTo("year", year)
                            .get()
                            .addOnSuccessListener(attSnap -> {
                                Map<String, Integer> attendanceCount = new HashMap<>();
                                for (com.google.firebase.firestore.DocumentSnapshot doc : attSnap.getDocuments()) {
                                    String uid = doc.getString("userId");
                                    if (uid != null) {
                                        attendanceCount.put(uid, attendanceCount.getOrDefault(uid, 0) + 1);
                                    }
                                }

                                for (User emp : employees) {
                                    int daysPresent = attendanceCount.getOrDefault(emp.getUid(), 0);
                                    double score = Math.min(emp.getProductivityScore(), 100.0);
                                    double attendanceRatio = Math.min((double) daysPresent / WORKING_DAYS, 1.0);
                                    double productivityBonus = score / 100.0 * MAX_PRODUCTIVITY_BONUS;
                                    double finalSalary = emp.getBaseSalary() * attendanceRatio * (1.0 + productivityBonus);

                                    salaryList.add(new SalaryRecord(
                                            emp.getFullName(),
                                            emp.getBaseSalary(),
                                            daysPresent,
                                            score,
                                            finalSalary
                                    ));
                                }

                                salaryList.sort((a, b) -> Double.compare(b.finalSalary, a.finalSalary));
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> adapter.notifyDataSetChanged());
                });
    }

    static class SalaryRecord {
        String name;
        double baseSalary, productivityScore, finalSalary;
        int daysPresent;

        SalaryRecord(String name, double baseSalary, int daysPresent, double productivityScore, double finalSalary) {
            this.name = name;
            this.baseSalary = baseSalary;
            this.daysPresent = daysPresent;
            this.productivityScore = productivityScore;
            this.finalSalary = finalSalary;
        }
    }

    class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.VH> {
        private final List<SalaryRecord> list;
        SalaryAdapter(List<SalaryRecord> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salary, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SalaryRecord r = list.get(position);
            h.tvName.setText(r.name);
            h.tvFinalSalary.setText(currencyFormat.format((long) r.finalSalary) + " VNĐ");
            h.tvDetails.setText(String.format(Locale.getDefault(),
                    "LCB: %s | Ngày: %d/%d | NS: %.0f%%",
                    currencyFormat.format((long) r.baseSalary),
                    r.daysPresent, WORKING_DAYS, r.productivityScore));
            double bonusPct = r.productivityScore / 100.0 * MAX_PRODUCTIVITY_BONUS * 100;
            h.tvBonus.setText(String.format(Locale.getDefault(), "+%.1f%%", bonusPct));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvFinalSalary, tvDetails, tvBonus;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvEmployeeName);
                tvFinalSalary = v.findViewById(R.id.tvFinalSalary);
                tvDetails = v.findViewById(R.id.tvSalaryDetails);
                tvBonus = v.findViewById(R.id.tvProductivityBonus);
            }
        }
    }
}
