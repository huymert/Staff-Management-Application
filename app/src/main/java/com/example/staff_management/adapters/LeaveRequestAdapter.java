package com.example.staff_management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.R;
import com.example.staff_management.models.LeaveRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.LeaveRequestViewHolder> {

    /*
     * LeaveActionListener:
     * - Dùng để: báo lại thao tác duyệt hoặc từ chối đơn nghỉ.
     * - Kiểm tra/xử lý: tách xử lý click khỏi adapter.
     * - Sau đó: activity chính sẽ cập nhật Firestore.
     */
    public interface LeaveActionListener {
        void onApprove(LeaveRequest request);
        void onReject(LeaveRequest request);
    }

    private final List<LeaveRequest> requests;
    private final boolean canReview;
    private final LeaveActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /*
     * LeaveRequestAdapter:
     * - Dùng để: hiển thị danh sách đơn nghỉ phép.
     * - Kiểm tra/xử lý: bind thông tin người tạo, ngày nghỉ, trạng thái và nút duyệt.
     * - Sau đó: màn hình có thể xem và xử lý đơn ngay trên list.
     */
    public LeaveRequestAdapter(List<LeaveRequest> requests, boolean canReview, LeaveActionListener listener) {
        this.requests = requests;
        this.canReview = canReview;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LeaveRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leave_request, parent, false);
        return new LeaveRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaveRequestViewHolder holder, int position) {
        LeaveRequest request = requests.get(position);
        holder.tvEmployeeName.setText(holder.itemView.getContext().getString(
                R.string.leave_request_by,
                safe(request.getUserName()),
                safe(request.getDepartment())
        ));
        holder.tvLeaveType.setText(request.getLeaveType());
        holder.tvLeaveDate.setText(holder.itemView.getContext().getString(
                R.string.leave_request_date,
                request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "--",
                request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "--"
        ));
        holder.tvReason.setText(holder.itemView.getContext().getString(
                R.string.leave_request_reason_label,
                safe(request.getReason())
        ));
        holder.tvCreatedAt.setText(holder.itemView.getContext().getString(
                R.string.leave_request_created,
                request.getCreatedAt() != null ? dateTimeFormat.format(request.getCreatedAt()) : "--"
        ));
        holder.tvDays.setText(holder.itemView.getContext().getString(
                R.string.leave_days_count,
                Math.max(1, request.getTotalDays())
        ));

        bindStatus(holder, request);

        boolean isPending = "pending".equalsIgnoreCase(request.getStatus());
        holder.btnApprove.setVisibility(canReview && isPending ? View.VISIBLE : View.GONE);
        holder.btnReject.setVisibility(canReview && isPending ? View.VISIBLE : View.GONE);
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(request));
        holder.btnReject.setOnClickListener(v -> listener.onReject(request));
    }

    /*
     * bindStatus:
     * - Dùng để: hiển thị nhãn trạng thái của đơn nghỉ.
     * - Kiểm tra/xử lý: đổi badge theo approved, rejected hay pending.
     * - Sau đó: người xem biết ngay đơn đang ở bước nào.
     */
    private void bindStatus(LeaveRequestViewHolder holder, LeaveRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().toLowerCase(Locale.ROOT);
        int textRes;
        int backgroundRes;
        switch (status) {
            case "approved":
                textRes = R.string.leave_status_approved;
                backgroundRes = R.drawable.bg_badge_success;
                break;
            case "rejected":
                textRes = R.string.leave_status_rejected;
                backgroundRes = R.drawable.bg_badge_error;
                break;
            default:
                textRes = R.string.leave_status_pending;
                backgroundRes = R.drawable.bg_badge_warning;
                break;
        }
        holder.tvStatus.setText(textRes);
        holder.tvStatus.setBackgroundResource(backgroundRes);

        if ("rejected".equals(status) && request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
            holder.tvMeta.setVisibility(View.VISIBLE);
            holder.tvMeta.setText(holder.itemView.getContext().getString(
                    R.string.leave_rejection_reason_label,
                    request.getRejectionReason()
            ));
        } else if (request.getApproverName() != null && !request.getApproverName().isEmpty()
                && !"pending".equals(status)) {
            holder.tvMeta.setVisibility(View.VISIBLE);
            holder.tvMeta.setText(holder.itemView.getContext().getString(
                    R.string.leave_approver_label,
                    request.getApproverName()
            ));
        } else {
            holder.tvMeta.setVisibility(View.GONE);
        }
    }

    private String safe(String value) {
        return value == null || value.isEmpty()
                ? ""
                : value;
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class LeaveRequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmployeeName;
        private final TextView tvLeaveType;
        private final TextView tvLeaveDate;
        private final TextView tvReason;
        private final TextView tvCreatedAt;
        private final TextView tvDays;
        private final TextView tvStatus;
        private final TextView tvMeta;
        private final MaterialButton btnApprove;
        private final MaterialButton btnReject;

        public LeaveRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvLeaveEmployeeName);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvLeaveDate = itemView.findViewById(R.id.tvLeaveDate);
            tvReason = itemView.findViewById(R.id.tvLeaveReason);
            tvCreatedAt = itemView.findViewById(R.id.tvLeaveCreatedAt);
            tvDays = itemView.findViewById(R.id.tvLeaveDays);
            tvStatus = itemView.findViewById(R.id.tvLeaveStatus);
            tvMeta = itemView.findViewById(R.id.tvLeaveMeta);
            btnApprove = itemView.findViewById(R.id.btnApproveLeave);
            btnReject = itemView.findViewById(R.id.btnRejectLeave);
        }
    }
}
