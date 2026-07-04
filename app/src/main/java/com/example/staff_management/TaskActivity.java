package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.staff_management.adapters.TaskAdapter;
import com.example.staff_management.models.Task;
import com.example.staff_management.viewmodel.TaskListViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends BaseActivity {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private BottomNavigationView bottomNavigation;
    private TaskListViewModel viewModel;
    private String currentUserId;

    /*
     * onCreate:
     * - Dùng để: mở màn hình danh sách task của nhân viên.
     * - Kiểm tra/xử lý: lấy user hiện tại, gắn RecyclerView và observe dữ liệu từ ViewModel.
     * - Sau đó: hiển thị task và cho người dùng chuyển sang các màn hình khác từ bottom nav.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        viewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
        FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        currentUserId = currentUser.getUid();

        rvTasks = findViewById(R.id.rvTasks);
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);

        setupBottomNavigation();

        View headerLayout = findViewById(R.id.headerLayout);
        if (headerLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerLayout, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), statusBarHeight + (int) (8 * getResources().getDisplayMetrics().density), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        viewModel.getTasks().observe(this, tasks -> {
            taskList.clear();
            taskList.addAll(tasks);
            adapter.notifyDataSetChanged();
        });
        viewModel.getError().observe(this, errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.loadTasksForEmployee(currentUserId);
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho màn hình task.
     * - Kiểm tra/xử lý: bắt sự kiện từng tab để chuyển sang home, chat hoặc AI assistant.
     * - Sau đó: người dùng đi tới đúng màn hình mà không phải quay lại thủ công.
     */
    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_task);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_messenger) {
                startActivity(new Intent(this, UserListChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_task) {
                return true;
            } else if (id == R.id.nav_ai_chat) {
                showAiAssistantDialog();
                return true;
            }
            return false;
        });
    }
}
