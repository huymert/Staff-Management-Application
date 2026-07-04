package com.example.staff_management.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public final class FirebaseRepository {

    private FirebaseRepository() {}

    /*
     * db:
     * - Dùng để: lấy instance Firestore dùng chung.
     * - Kiểm tra/xử lý: gom truy cập database về một chỗ.
     * - Sau đó: code ở các màn khác gọi lại cho gọn hơn.
     */
    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    /*
     * users:
     * - Dùng để: trỏ tới collection Users.
     * - Kiểm tra/xử lý: không phải nơi xử lý logic, chỉ trả về reference.
     * - Sau đó: các màn hình khác dùng để đọc hoặc ghi user.
     */
    public static CollectionReference users() {
        return db().collection("Users");
    }

    /*
     * tasks:
     * - Dùng để: trỏ tới collection Tasks.
     * - Kiểm tra/xử lý: gom reference cho phần task.
     * - Sau đó: màn task và tính năng liên quan dùng chung một nguồn.
     */
    public static CollectionReference tasks() {
        return db().collection("Tasks");
    }

    /*
     * attendance:
     * - Dùng để: trỏ tới collection Attendance.
     * - Kiểm tra/xử lý: gom reference cho phần chấm công.
     * - Sau đó: các màn điểm danh và lương dùng lại cho đồng nhất.
     */
    public static CollectionReference attendance() {
        return db().collection("Attendance");
    }

    /*
     * user:
     * - Dùng để: lấy document của một user cụ thể.
     * - Kiểm tra/xử lý: ghép userId vào path Users/{userId}.
     * - Sau đó: những hàm update/delete sẽ gọi lại cho nhanh.
     */
    public static DocumentReference user(String userId) {
        return users().document(userId);
    }

    /*
     * updatePresence:
     * - Dùng để: cập nhật trạng thái online/offline cho user.
     * - Kiểm tra/xử lý: update field presenceStatus trên Firestore.
     * - Sau đó: chat và danh sách nhân sự sẽ thấy trạng thái mới.
     */
    public static Task<Void> updatePresence(String userId, String presenceStatus) {
        return user(userId).update("presenceStatus", presenceStatus);
    }

    /*
     * deactivateEmployee:
     * - Dùng để: khóa mềm một nhân viên.
     * - Kiểm tra/xử lý: đổi trạng thái nghỉ việc và gỡ token, biometric.
     * - Sau đó: tài khoản vẫn còn nhưng không hoạt động nữa.
     */
    public static Task<Void> deactivateEmployee(String userId) {
        WriteBatch batch = db().batch();
        batch.update(user(userId),
                "employmentStatus", "Nghỉ việc",
                "status", "Nghỉ việc",
                "presenceStatus", "offline",
                "biometricRegistered", false,
                "fcmToken", FieldValue.delete());
        return batch.commit();
    }

    /*
     * hardDeleteEmployeeData:
     * - Dùng để: xóa hẳn nhân viên và dữ liệu liên quan.
     * - Kiểm tra/xử lý: lấy attendance và task trước rồi xóa theo batch.
     * - Sau đó: document user và dữ liệu gắn với họ biến mất khỏi Firestore.
     */
    public static Task<Void> hardDeleteEmployeeData(String userId) {
        Task<QuerySnapshot> attendanceTask = attendance().whereEqualTo("userId", userId).get();
        Task<QuerySnapshot> employeeTasksTask = tasks().whereEqualTo("employeeId", userId).get();
        Task<QuerySnapshot> assignedTasksTask = tasks().whereEqualTo("adminId", userId).get();

        return Tasks.whenAllSuccess(attendanceTask, employeeTasksTask, assignedTasksTask)
                .onSuccessTask(results -> {
                    List<QuerySnapshot> snapshots = new ArrayList<>();
                    for (Object result : results) {
                        snapshots.add((QuerySnapshot) result);
                    }

                    WriteBatch batch = db().batch();
                    batch.delete(user(userId));

                    for (QuerySnapshot snapshot : snapshots) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                    }

                    return batch.commit();
                });
    }
}
