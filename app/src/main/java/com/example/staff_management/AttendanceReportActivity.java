package com.example.staff_management;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceReportActivity extends BaseActivity {

    private TextView tvMonthYear, tvDaysPresent, tvTotalHours, tvAvgHours;
    private RecyclerView rvAttendanceDetails;
    private FirebaseFirestore db;
    private String employeeId;
    private Calendar calendar;
    private DetailAdapter detailAdapter;
    private List<AttendanceDetail> detailList = new ArrayList<>();

    /*
     * onCreate:
     * - Dùng để: mở màn hình báo cáo chuyên cần của một nhân viên.
     * - Kiểm tra/xử lý: lấy employeeId từ intent và set RecyclerView.
     * - Sau đó: gọi loadReport để lấy dữ liệu theo tháng.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_report);

        db = FirebaseFirestore.getInstance();
        employeeId = getIntent().getStringExtra("employeeId");
        String employeeName = getIntent().getStringExtra("employeeName");
        if (employeeId == null || employeeId.isEmpty()) {
            finish();
            return;
        }

        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvDaysPresent = findViewById(R.id.tvDaysPresent);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        tvAvgHours = findViewById(R.id.tvAvgHours);
        rvAttendanceDetails = findViewById(R.id.rvAttendanceDetails);

        calendar = Calendar.getInstance();
        detailAdapter = new DetailAdapter(detailList);
        rvAttendanceDetails.setLayoutManager(new LinearLayoutManager(this));
        rvAttendanceDetails.setAdapter(detailAdapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnPrev = findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = findViewById(R.id.btnNextMonth);

        btnBack.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> { calendar.add(Calendar.MONTH, -1); loadReport(); });
        btnNext.setOnClickListener(v -> { calendar.add(Calendar.MONTH, 1); loadReport(); });

        loadReport();
    }

    /*
     * loadReport:
     * - Dùng để: tải dữ liệu chấm công theo tháng.
     * - Kiểm tra/xử lý: đếm số ngày đi làm, tổng giờ và giờ trung bình.
     * - Sau đó: đổ dữ liệu ra phần tóm tắt và danh sách chi tiết.
     */
    private void loadReport() {
        SimpleDateFormat sdfTitle = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdfTitle.format(calendar.getTime()));

        String monthPrefix = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(calendar.getTime());

        db.collection("Attendance")
                .whereGreaterThanOrEqualTo("__name__", employeeId + "_" + monthPrefix + "01")
                .whereLessThanOrEqualTo("__name__", employeeId + "_" + monthPrefix + "31")
                .get()
                .addOnSuccessListener(snap -> {
                    detailList.clear();
                    int totalMinutes = 0;
                    int daysPresent = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Date checkIn = doc.getDate("checkIn");
                        Date checkOut = doc.getDate("checkOut");
                        if (checkIn == null) continue;

                        daysPresent++;
                        long workMinutes = 0;
                        if (checkOut != null) {
                            workMinutes = (checkOut.getTime() - checkIn.getTime()) / 60000;
                            totalMinutes += workMinutes;
                        }

                        String dateStr = new SimpleDateFormat("dd/MM/yyyy (EEE)", Locale.getDefault()).format(checkIn);
                        String inStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkIn);
                        String outStr = checkOut != null
                                ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkOut)
                                : "--:--";
                        String durationStr = checkOut != null
                                ? String.format(Locale.getDefault(), "%dh %02dm", workMinutes / 60, workMinutes % 60)
                                : "Chưa checkout";

                        detailList.add(new AttendanceDetail(dateStr, inStr, outStr, durationStr));
                    }

                    tvDaysPresent.setText(String.valueOf(daysPresent));
                    long totalH = totalMinutes / 60;
                    long totalM = totalMinutes % 60;
                    tvTotalHours.setText(totalH + "h" + (totalM > 0 ? totalM + "m" : ""));
                    if (daysPresent > 0) {
                        long avgMin = totalMinutes / daysPresent;
                        tvAvgHours.setText(avgMin / 60 + "h" + (avgMin % 60 > 0 ? avgMin % 60 + "m" : ""));
                    } else {
                        tvAvgHours.setText("0h");
                    }

                    detailAdapter.notifyDataSetChanged();
                });
    }

    static class AttendanceDetail {
        String date, checkIn, checkOut, duration;
        AttendanceDetail(String date, String checkIn, String checkOut, String duration) {
            this.date = date; this.checkIn = checkIn; this.checkOut = checkOut; this.duration = duration;
        }
    }

    static class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.VH> {
        private final List<AttendanceDetail> list;
        DetailAdapter(List<AttendanceDetail> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AttendanceDetail d = list.get(position);
            holder.text1.setText(d.date + "  |  " + d.checkIn + " → " + d.checkOut);
            holder.text2.setText("Thời gian làm việc: " + d.duration);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
