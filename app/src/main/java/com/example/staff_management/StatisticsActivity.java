package com.example.staff_management;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.staff_management.models.User;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends BaseActivity {

    private BarChart barChartDepartment;
    private PieChart pieChartStatus;
    private TextView tvTotalEmployees;
    private TextView tvNewHires;
    private FirebaseFirestore db;

    /*
     * onCreate:
     * - Dùng để: khởi tạo màn hình thống kê và tải dữ liệu lên biểu đồ.
     * - Kiểm tra/xử lý: lấy dữ liệu nhân viên từ Firestore, rồi dựng layout theo safe area.
     * - Sau đó: set menu bottom nav và gọi hàm lấy dữ liệu thống kê.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        db = FirebaseFirestore.getInstance();
        barChartDepartment = findViewById(R.id.barChartDepartment);
        pieChartStatus = findViewById(R.id.pieChartStatus);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvNewHires = findViewById(R.id.tvNewHires);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.statisticsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int) (70 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                        headerContent.getPaddingLeft(),
                        systemBars.top + (int) (4 * getResources().getDisplayMetrics().density),
                        headerContent.getPaddingRight(),
                        headerContent.getPaddingBottom()
                );
            }

            View footerContainer = findViewById(R.id.footerContainer);
            if (footerContainer != null) {
                footerContainer.setPadding(
                        footerContainer.getPaddingLeft(),
                        footerContainer.getPaddingTop(),
                        footerContainer.getPaddingRight(),
                        systemBars.bottom + (int) (6 * getResources().getDisplayMetrics().density)
                );
            }

            return WindowInsetsCompat.CONSUMED;
        });

        setupBottomNavigation();
        fetchData();
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: gắn menu dưới cho màn hình thống kê.
     * - Kiểm tra/xử lý: xử lý chuyển trang home, chat và AI.
     * - Sau đó: người dùng bấm là đi đúng màn hình tương ứng.
     */
    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_stats);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_messenger) {
                startActivity(new Intent(this, UserListChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_stats) {
                return true;
            } else if (id == R.id.nav_ai_chat) {
                showAiAssistantDialog();
                return true;
            }
            return false;
        });
    }

    /*
     * fetchData:
     * - Dùng để: lấy dữ liệu nhân sự từ Firestore cho phần thống kê.
     * - Kiểm tra/xử lý: đếm theo phòng ban, trạng thái làm việc và số nhân viên mới trong tháng.
     * - Sau đó: đẩy kết quả sang biểu đồ và các ô tổng quan.
     */
    private void fetchData() {
        db.collection("Users")
                .whereNotEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> deptCount = new HashMap<>();
                    Map<String, Integer> statusCount = new HashMap<>();
                    int newHires = 0;
                    int totalUsers = 0;

                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    String currentMonthYear = String.format(Locale.getDefault(), "%02d/%d",
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.YEAR));

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user == null || "admin".equalsIgnoreCase(user.getRole())) {
                            continue;
                        }

                        totalUsers++;

                        String dept = user.getDepartment();
                        if (dept == null || dept.isEmpty()) {
                            dept = getString(R.string.other_department);
                        }
                        deptCount.put(dept, deptCount.getOrDefault(dept, 0) + 1);

                        String status = normalizeEmploymentStatus(user.getEmploymentStatus());
                        statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

                        if (user.getJoinDate() != null && user.getJoinDate().contains(currentMonthYear)) {
                            newHires++;
                        }
                    }

                    tvTotalEmployees.setText(String.valueOf(totalUsers));
                    tvNewHires.setText(String.valueOf(newHires));

                    setupBarChart(deptCount);
                    setupPieChart(statusCount);
                });
    }

    /*
     * setupBarChart:
     * - Dùng để: dựng biểu đồ cột theo phòng ban.
     * - Kiểm tra/xử lý: đổi map dữ liệu thành entry cho MPAndroidChart.
     * - Sau đó: biểu đồ hiển thị số lượng nhân viên từng phòng ban.
     */
    private void setupBarChart(Map<String, Integer> deptCount) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : deptCount.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            labels.add(entry.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#24B5D3"));
        dataSet.setValueTextSize(11f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#12313A"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.4f);

        barChartDepartment.setData(data);
        barChartDepartment.getDescription().setEnabled(false);
        barChartDepartment.getLegend().setEnabled(false);
        barChartDepartment.setDrawGridBackground(false);
        barChartDepartment.setDrawBarShadow(false);

        XAxis xAxis = barChartDepartment.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#D8F8FC"));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#6F9CAB"));

        barChartDepartment.getAxisLeft().setEnabled(false);
        barChartDepartment.getAxisRight().setEnabled(false);
        barChartDepartment.setScaleEnabled(false);
        barChartDepartment.setTouchEnabled(true);
        barChartDepartment.animateY(1500);
        barChartDepartment.invalidate();
    }

    /*
     * setupPieChart:
     * - Dùng để: dựng biểu đồ tròn theo trạng thái làm việc.
     * - Kiểm tra/xử lý: gom số lượng active, probation và resigned.
     * - Sau đó: hiện tỷ lệ phần trăm trực quan cho màn hình thống kê.
     */
    private void setupPieChart(Map<String, Integer> statusCount) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        String[] statuses = {
                getString(R.string.employment_active),
                getString(R.string.employment_probation),
                getString(R.string.employment_resigned)
        };
        String[] colorCodes = {"#66BB6A", "#FFB300", "#EF5350"};

        for (int i = 0; i < statuses.length; i++) {
            String status = statuses[i];
            int count = statusCount.getOrDefault(status, 0);
            if (count > 0) {
                entries.add(new PieEntry(count, status));
                colors.add(Color.parseColor(colorCodes[i]));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(4f);
        dataSet.setSelectionShift(7f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new PercentFormatter(pieChartStatus));

        PieData data = new PieData(dataSet);
        pieChartStatus.setData(data);
        pieChartStatus.setUsePercentValues(true);
        pieChartStatus.getDescription().setEnabled(false);
        pieChartStatus.setHoleRadius(65f);
        pieChartStatus.setTransparentCircleRadius(70f);
        pieChartStatus.setHoleColor(Color.TRANSPARENT);
        pieChartStatus.setDrawEntryLabels(false);

        Legend legend = pieChartStatus.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setTextColor(Color.parseColor("#6F9CAB"));
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setYEntrySpace(10f);

        pieChartStatus.setCenterText(getString(R.string.stats_center_text));
        pieChartStatus.setCenterTextColor(Color.parseColor("#12313A"));
        pieChartStatus.setCenterTextSize(13f);
        pieChartStatus.setExtraOffsets(0, 5, 10, 5);
        pieChartStatus.animateY(1400, Easing.EaseInOutQuad);
        pieChartStatus.invalidate();
    }

    /*
     * normalizeEmploymentStatus:
     * - Dùng để: chuẩn hóa nhiều kiểu chuỗi trạng thái về một nhãn chung.
     * - Kiểm tra/xử lý: đọc cả tiếng Anh và tiếng Việt để tránh dữ liệu bị lệch format.
     * - Sau đó: biểu đồ và số liệu luôn dùng cùng một chuẩn hiển thị.
     */
    private String normalizeEmploymentStatus(String rawStatus) {
        String safe = rawStatus == null ? "" : rawStatus.toLowerCase(Locale.ROOT);
        if (safe.contains("probation") || safe.contains("thử việc") || safe.contains("thu viec")) {
            return getString(R.string.employment_probation);
        }
        if (safe.contains("resigned") || safe.contains("nghỉ việc") || safe.contains("nghi viec")) {
            return getString(R.string.employment_resigned);
        }
        if (safe.contains("active") || safe.contains("đang làm") || safe.contains("dang lam")) {
            return getString(R.string.employment_active);
        }
        return getString(R.string.not_available);
    }
}
