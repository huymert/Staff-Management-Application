package com.example.staff_management.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Locale;

public class ProductivityCalculator {

    private static final int WORKING_DAYS_PER_MONTH = 26;

    /*
     * recalculate:
     * - Dùng để: tính lại điểm năng suất cho một nhân viên.
     * - Kiểm tra/xử lý: đếm task hoàn thành và ngày công trong tháng rồi ghép thành một điểm tổng.
     * - Sau đó: lưu lại productivityScore vào Firestore để màn hình khác lấy tiếp.
     */
    public static void recalculate(String userId, FirebaseFirestore db) {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String monthPrefix = String.format(Locale.getDefault(), "%04d%02d", year, month);

        db.collection("Tasks")
                .whereEqualTo("employeeId", userId)
                .get()
                .addOnSuccessListener(taskSnap -> {
                    int total = 0, completed = 0;
                    long startOfMonth = getStartOfMonth(year, month - 1);
                    long endOfMonth = getEndOfMonth(year, month - 1);

                    for (QueryDocumentSnapshot doc : taskSnap) {
                        Long ts = doc.getLong("timestamp");
                        if (ts == null || ts < startOfMonth || ts > endOfMonth) continue;
                        total++;
                        String status = doc.getString("status");
                        if ("Completed".equalsIgnoreCase(status)) completed++;
                    }

                    final double taskRate = (total == 0) ? 100.0 : (completed * 100.0 / total);

                    db.collection("Attendance")
                            .whereGreaterThanOrEqualTo("__name__", userId + "_" + monthPrefix + "01")
                            .whereLessThanOrEqualTo("__name__", userId + "_" + monthPrefix + "31")
                            .get()
                            .addOnSuccessListener(attSnap -> {
                                int daysPresent = attSnap.size();
                                double attendanceRate = Math.min(daysPresent * 100.0 / WORKING_DAYS_PER_MONTH, 100.0);

                                double score = Math.min(taskRate * 0.6 + attendanceRate * 0.4, 100.0);
                                score = Math.round(score * 10.0) / 10.0;

                                db.collection("Users").document(userId)
                                        .update("productivityScore", score);
                            });
                });
    }

    private static long getStartOfMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long getEndOfMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
