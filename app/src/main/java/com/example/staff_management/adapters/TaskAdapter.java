package com.example.staff_management.adapters;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.R;
import com.example.staff_management.models.Task;
import com.example.staff_management.utils.ProductivityCalculator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;

    /*
     * TaskAdapter:
     * - Dùng để: hiển thị danh sách task trong RecyclerView.
     * - Kiểm tra/xử lý: bind tiêu đề, trạng thái, deadline và người thực hiện.
     * - Sau đó: user có thể bấm vào task để đánh dấu hoàn thành.
     */
    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvObjective.setText(task.getObjective());
        holder.tvStatus.setText(displayStatus(holder, task.getStatus()));

        if (task.getEmployeeName() != null) {
            holder.tvEmployeeName.setText("Người thực hiện: " + task.getEmployeeName());
            holder.tvEmployeeName.setVisibility(View.VISIBLE);
        } else {
            holder.tvEmployeeName.setVisibility(View.GONE);
        }

        bindPriority(holder, task.getPriority());
        bindTime(holder, task);
        bindDeadline(holder, task);
        bindStatusStyle(holder, task.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (!isPending(task.getStatus())) {
                return;
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle(R.string.task_complete_title)
                    .setMessage(v.getContext().getString(R.string.task_complete_message, task.getTitle()))
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("Tasks").document(task.getId())
                                .update("status", "Completed")
                                .addOnSuccessListener(aVoid -> {
                                    task.setStatus("Completed");
                                    int adapterPosition = holder.getBindingAdapterPosition();
                                    if (adapterPosition != RecyclerView.NO_POSITION) {
                                        notifyItemChanged(adapterPosition);
                                    } else {
                                        notifyDataSetChanged();
                                    }
                                    Toast.makeText(v.getContext(), R.string.task_completed_success, Toast.LENGTH_SHORT).show();
                                    ProductivityCalculator.recalculate(task.getEmployeeId(), db);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(v.getContext(), v.getContext().getString(R.string.error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    /*
     * bindPriority:
     * - Dùng để: hiển thị mức ưu tiên của task.
     * - Kiểm tra/xử lý: đổi màu theo cao, trung bình hoặc thấp.
     * - Sau đó: người dùng nhìn là biết task nào cần làm trước.
     */
    private void bindPriority(TaskViewHolder holder, String priority) {
        String value = priority == null ? "" : priority.trim();
        if (value.isEmpty()) {
            value = holder.itemView.getContext().getString(R.string.priority_medium);
        }

        holder.tvPriority.setText(value);
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("high") || lower.contains("cao")) {
            holder.tvPriority.setBackgroundColor(Color.parseColor("#F44336"));
        } else if (lower.contains("low") || lower.contains("thấp") || lower.contains("thap")) {
            holder.tvPriority.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvPriority.setBackgroundColor(Color.parseColor("#FF9800"));
        }
    }

    /*
     * bindTime:
     * - Dùng để: hiển thị thời gian tạo task.
     * - Kiểm tra/xử lý: format timestamp sang chuỗi dễ đọc.
     * - Sau đó: item có thêm mốc thời gian để theo dõi.
     */
    private void bindTime(TaskViewHolder holder, Task task) {
        if (task.getTimestamp() > 0) {
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(task.getTimestamp()));
            holder.tvTime.setText(date);
        } else {
            holder.tvTime.setText("");
        }
    }

    /*
     * bindDeadline:
     * - Dùng để: hiện hạn chót của task.
     * - Kiểm tra/xử lý: so deadline với thời gian hiện tại để cảnh báo trễ hạn.
     * - Sau đó: task quá hạn sẽ nổi bật hơn trên giao diện.
     */
    private void bindDeadline(TaskViewHolder holder, Task task) {
        if (task.getDeadline() > 0) {
            String deadlineStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(task.getDeadline()));
            holder.tvDeadline.setText(holder.itemView.getContext().getString(R.string.task_deadline, deadlineStr));
            boolean isOverdue = task.getDeadline() < System.currentTimeMillis() && !isCompleted(task.getStatus());
            holder.tvDeadline.setTextColor(isOverdue ? Color.parseColor("#F44336") : Color.parseColor("#FF5722"));
            holder.tvDeadline.setVisibility(View.VISIBLE);
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
        }
    }

    /*
     * bindStatusStyle:
     * - Dùng để: đổi badge trạng thái cho task.
     * - Kiểm tra/xử lý: phân biệt pending và completed.
     * - Sau đó: item nhìn trực quan hơn.
     */
    private void bindStatusStyle(TaskViewHolder holder, String status) {
        if (isPending(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (isCompleted(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }
    }

    private boolean isPending(String status) {
        String safe = status == null ? "" : status.toLowerCase(Locale.ROOT);
        return safe.contains("pending") || safe.contains("chờ") || safe.contains("cho");
    }

    private boolean isCompleted(String status) {
        String safe = status == null ? "" : status.toLowerCase(Locale.ROOT);
        return safe.contains("completed") || safe.contains("hoàn thành") || safe.contains("hoan thanh");
    }

    private String displayStatus(TaskViewHolder holder, String status) {
        if (isCompleted(status)) {
            return holder.itemView.getContext().getString(R.string.completed_status);
        }
        if (isPending(status)) {
            return holder.itemView.getContext().getString(R.string.pending_status);
        }
        return status == null ? "" : status;
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvObjective;
        TextView tvStatus;
        TextView tvTime;
        TextView tvPriority;
        TextView tvDeadline;
        TextView tvEmployeeName;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvObjective = itemView.findViewById(R.id.tvTaskObjective);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTime = itemView.findViewById(R.id.tvTaskTime);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
        }
    }
}
