package com.example.staff_management.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.staff_management.models.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskListViewModel extends ViewModel {

    private final MutableLiveData<List<Task>> tasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;

    /*
     * getTasks:
     * - Dùng để: trả dữ liệu task cho UI quan sát.
     * - Kiểm tra/xử lý: không xử lý thêm, chỉ expose LiveData.
     * - Sau đó: Activity tự observe để tự refresh list.
     */
    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    /*
     * getError:
     * - Dùng để: trả thông báo lỗi nếu load task thất bại.
     * - Kiểm tra/xử lý: expose LiveData để UI show toast.
     * - Sau đó: màn hình biết được lỗi từ Firestore.
     */
    public LiveData<String> getError() {
        return error;
    }

    /*
     * loadTasksForEmployee:
     * - Dùng để: lấy task của nhân viên hiện tại.
     * - Kiểm tra/xử lý: lắng nghe collection Tasks theo employeeId.
     * - Sau đó: cập nhật list task realtime trên UI.
     */
    public void loadTasksForEmployee(String employeeId) {
        removeListener();
        tasks.setValue(new ArrayList<>());
        listenerRegistration = db.collection("Tasks")
                .whereEqualTo("employeeId", employeeId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        error.setValue(e.getMessage());
                        return;
                    }
                    updateFromSnapshot(value);
                });
    }

    /*
     * loadTasksAssignedBy:
     * - Dùng để: lấy task mà admin đã giao.
     * - Kiểm tra/xử lý: lắng nghe theo adminId và sắp xếp theo thời gian.
     * - Sau đó: manager/admin thấy lại danh sách task đã tạo.
     */
    public void loadTasksAssignedBy(String adminId) {
        removeListener();
        tasks.setValue(new ArrayList<>());
        listenerRegistration = db.collection("Tasks")
                .whereEqualTo("adminId", adminId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        error.setValue(e.getMessage());
                        return;
                    }
                    updateFromSnapshot(value);
                });
    }

    /*
     * updateFromSnapshot:
     * - Dùng để: đổi snapshot Firestore thành list Task.
     * - Kiểm tra/xử lý: set id từ document id rồi gom vào list mới.
     * - Sau đó: LiveData được update để RecyclerView vẽ lại.
     */
    private void updateFromSnapshot(com.google.firebase.firestore.QuerySnapshot value) {
        if (value == null) return;
        List<Task> list = new ArrayList<>();
        for (QueryDocumentSnapshot doc : value) {
            Task task = doc.toObject(Task.class);
            if (task != null) {
                task.setId(doc.getId());
                list.add(task);
            }
        }
        tasks.setValue(list);
    }

    private void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListener();
    }
}
