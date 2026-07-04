package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.staff_management.adapters.TaskAdapter;
import com.example.staff_management.models.Task;
import com.example.staff_management.viewmodel.TaskListViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ManagerTaskActivity extends BaseActivity {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddTask;
    private BottomNavigationView bottomNavigation;
    private TaskListViewModel viewModel;
    private String currentUserId;

    /*
     * onCreate:
     * - Dùng để: mở màn hình quản lý task của manager.
     * - Kiểm tra/xử lý: lấy user hiện tại, set tab, set list và tải dữ liệu task đúng loại.
     * - Sau đó: manager xem task đã giao hoặc task của mình và có thể thêm task mới.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_task);

        viewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
        FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        currentUserId = currentUser.getUid();

        rvTasks = findViewById(R.id.rvTasks);
        tabLayout = findViewById(R.id.tabLayout);
        fabAddTask = findViewById(R.id.fabAddTask);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.taskManagementRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            View headerBackground = findViewById(R.id.headerBackground);
            if (headerBackground != null) {
                android.view.ViewGroup.LayoutParams lp = headerBackground.getLayoutParams();
                lp.height = (int) (80 * getResources().getDisplayMetrics().density) + systemBars.top;
                headerBackground.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                        headerContent.getPaddingLeft(),
                        systemBars.top + (int) (6 * getResources().getDisplayMetrics().density),
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
                        systemBars.bottom + (int) (12 * getResources().getDisplayMetrics().density)
                );
            }

            if (fabAddTask != null) {
                android.view.ViewGroup.MarginLayoutParams fabLp = (android.view.ViewGroup.MarginLayoutParams) fabAddTask.getLayoutParams();
                fabLp.bottomMargin = systemBars.bottom + (int) (26 * getResources().getDisplayMetrics().density);
                fabAddTask.setLayoutParams(fabLp);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);

        viewModel.getTasks().observe(this, tasks -> {
            taskList.clear();
            if (tasks != null) {
                taskList.addAll(tasks);
            }
            adapter.notifyDataSetChanged();
            if (!taskList.isEmpty()) {
                rvTasks.scrollToPosition(0);
            }
        });

        setupTabs();
        setupBottomNavigation();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmployeeListActivity.class);
            intent.putExtra("mode", "productivity");
            intent.putExtra("role", "manager");
            startActivity(intent);
        });

        if (tabLayout.getSelectedTabPosition() == 0) {
            viewModel.loadTasksAssignedBy(currentUserId);
        } else {
            viewModel.loadTasksForEmployee(currentUserId);
        }
    }

    /*
     * setupTabs:
     * - Dùng để: đổi giữa task đã giao và task của chính manager.
     * - Kiểm tra/xử lý: nghe sự kiện chọn tab và reload dữ liệu tương ứng từ Firestore.
     * - Sau đó: danh sách task cập nhật đúng theo ngữ cảnh đang xem.
     */
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                taskList.clear();
                adapter.notifyDataSetChanged();

                if (tab.getPosition() == 0) {
                    viewModel.loadTasksAssignedBy(currentUserId);
                } else {
                    viewModel.loadTasksForEmployee(currentUserId);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    viewModel.loadTasksAssignedBy(currentUserId);
                } else {
                    viewModel.loadTasksForEmployee(currentUserId);
                }
            }
        });
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới cho màn hình task manager.
     * - Kiểm tra/xử lý: xử lý các tab home, chat, team, task và AI.
     * - Sau đó: chuyển màn hình đúng luồng làm việc của manager.
     */
    private void setupBottomNavigation() {
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
            } else if (id == R.id.nav_my_team) {
                Intent intent = new Intent(this, EmployeeListActivity.class);
                intent.putExtra("mode", "productivity");
                intent.putExtra("role", "manager");
                startActivity(intent);
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
