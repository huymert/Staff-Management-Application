package com.example.staff_management.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.EmployeeListActivity;
import com.example.staff_management.R;
import com.example.staff_management.models.User;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private final List<User> employeeList;
    private final OnItemClickListener listener;
    private final String mode;
    private Map<String, TaskSummary> taskSummaries = Collections.emptyMap();

    /*
     * TaskSummary:
     * - Dùng để: gom thông tin task mới nhất của một nhân viên.
     * - Kiểm tra/xử lý: lưu title, status và timestamp để hiển thị nhanh.
     * - Sau đó: list nhân viên có thể show thêm trạng thái task.
     */
    public static class TaskSummary {
        private final String title;
        private final String status;
        private final long timestamp;

        public TaskSummary(String title, String status, long timestamp) {
            this.title = title;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }

    /*
     * OnItemClickListener:
     * - Dùng để: báo lại khi bấm vào một nhân viên.
     * - Kiểm tra/xử lý: tách sự kiện click khỏi adapter.
     * - Sau đó: activity ngoài sẽ quyết định mở chi tiết hay xử lý khác.
     */
    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public EmployeeAdapter(List<User> employeeList, String mode, OnItemClickListener listener) {
        this.employeeList = employeeList;
        this.mode = mode;
        this.listener = listener;
    }

    /*
     * setTaskSummaries:
     * - Dùng để: cập nhật dữ liệu task mới nhất cho từng nhân viên.
     * - Kiểm tra/xử lý: nhận map rồi refresh RecyclerView.
     * - Sau đó: item hiển thị lại đúng trạng thái task.
     */
    public void setTaskSummaries(Map<String, TaskSummary> taskSummaries) {
        this.taskSummaries = taskSummaries != null ? taskSummaries : Collections.emptyMap();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User user = employeeList.get(position);
        holder.tvName.setText(user.getFullName());
        holder.tvEmail.setText(user.getEmail());

        bindTaskState(holder, user);
        bindSalaryOrPresence(holder, user);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
    }

    /*
     * bindTaskState:
     * - Dùng để: gắn trạng thái task cho từng nhân viên.
     * - Kiểm tra/xử lý: xem đang ở tab task nào và task đó đã hoàn thành chưa.
     * - Sau đó: item sẽ đổi icon và màu cho dễ nhìn.
     */
    private void bindTaskState(@NonNull EmployeeViewHolder holder, User user) {
        holder.ivTaskStatus.setVisibility(View.GONE);
        holder.tvTaskTitle.setVisibility(View.GONE);

        if (!"productivity".equals(mode)) {
            return;
        }

        int selectedTab = 0;
        if (holder.itemView.getContext() instanceof EmployeeListActivity) {
            com.google.android.material.tabs.TabLayout tabs =
                    ((EmployeeListActivity) holder.itemView.getContext()).findViewById(R.id.tabLayout);
            if (tabs != null) {
                selectedTab = tabs.getSelectedTabPosition();
            }
        }

        if (selectedTab != 1) {
            return;
        }

        holder.tvTaskTitle.setVisibility(View.VISIBLE);
        TaskSummary summary = taskSummaries.get(user.getUid());
        if (summary == null) {
            holder.tvTaskTitle.setText(R.string.no_task_assigned);
            holder.ivTaskStatus.setVisibility(View.VISIBLE);
            holder.ivTaskStatus.setImageResource(android.R.drawable.ic_delete);
            holder.ivTaskStatus.setColorFilter(android.graphics.Color.RED);
            return;
        }

        String timeSuffix = "";
        if (summary.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            timeSuffix = holder.itemView.getContext().getString(
                    R.string.date_time_suffix,
                    sdf.format(new java.util.Date(summary.getTimestamp()))
            );
        }

        holder.tvTaskTitle.setText(holder.itemView.getContext().getString(
                R.string.task_summary_text,
                summary.getTitle(),
                timeSuffix
        ));
        holder.ivTaskStatus.setVisibility(View.VISIBLE);

        boolean completed = "Completed".equalsIgnoreCase(summary.getStatus())
                || holder.itemView.getContext().getString(R.string.completed_status).equalsIgnoreCase(summary.getStatus());
        if (completed) {
            holder.ivTaskStatus.setImageResource(R.drawable.ic_check_no_frame);
            holder.ivTaskStatus.setColorFilter(android.graphics.Color.parseColor("#4CAF50"));
            holder.tvTaskTitle.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            holder.ivTaskStatus.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            holder.ivTaskStatus.setColorFilter(android.graphics.Color.RED);
            holder.tvTaskTitle.setTextColor(android.graphics.Color.parseColor("#007BFF"));
        }
    }

    /*
     * bindSalaryOrPresence:
     * - Dùng để: hiển thị lương hoặc trạng thái online tùy mode.
     * - Kiểm tra/xử lý: so mode salary hay các mode khác.
     * - Sau đó: item sẽ hiện đúng dữ liệu theo màn hình đang mở.
     */
    private void bindSalaryOrPresence(@NonNull EmployeeViewHolder holder, User user) {
        if ("salary".equals(mode)) {
            holder.tvSalary.setVisibility(View.VISIBLE);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
            holder.tvSalary.setText(holder.itemView.getContext().getString(
                    R.string.salary_currency_format,
                    formatter.format(user.getBaseSalary())
            ));
            holder.statusIndicator.setVisibility(View.GONE);
        } else {
            holder.tvSalary.setVisibility(View.GONE);
            holder.statusIndicator.setVisibility(View.VISIBLE);
            holder.statusIndicator.setBackgroundColor(
                    "online".equalsIgnoreCase(user.getPresenceStatus()) ? Color.GREEN : Color.LTGRAY
            );
        }
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvSalary;
        TextView tvTaskTitle;
        View statusIndicator;
        android.widget.ImageView ivTaskStatus;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmail = itemView.findViewById(R.id.tvEmployeeEmail);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            ivTaskStatus = itemView.findViewById(R.id.ivTaskStatus);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
        }
    }
}
