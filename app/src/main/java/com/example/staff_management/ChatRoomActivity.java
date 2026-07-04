package com.example.staff_management;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.staff_management.models.Message;
import com.example.staff_management.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Màn hình phòng chat 1-1 giữa hai người dùng.
 * Hỗ trợ phân trang tin nhắn, lắng nghe realtime qua Firestore snapshot listener,
 * đánh dấu đã xem (seen), và gửi thông báo đẩy cho người nhận.
 */
public class ChatRoomActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvTitle, tvReceiverPresence;
    private FirebaseFirestore db;
    private String currentUserId, receiverId, receiverName;
    private String currentUserName = "Anonymous";
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private View layoutMessageInput;
    private TextView tvStatusRestriction;
    private View header, headerContent;

    private static final int PAGE_SIZE = 30;
    private com.google.firebase.firestore.DocumentSnapshot oldestDocument = null;
    private boolean isLoadingMore = false;
    private boolean allLoaded = false;
    private ListenerRegistration messageListener, presenceListener;

    /*
     * onCreate:
     * - Dùng để: khởi tạo phòng chat 1-1.
     * - Kiểm tra/xử lý: xác thực user, load tin nhắn cũ và mở listener realtime.
     * - Sau đó: user có thể xem trạng thái online và gửi tin nhắn mới.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        currentUserId = currentUser.getUid();
        
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");

        if (receiverId == null) {
            Toast.makeText(this, "Không thể xác định người nhận", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvReceiverPresence = findViewById(R.id.tvReceiverPresence);
        header = findViewById(R.id.header);
        layoutMessageInput = findViewById(R.id.layoutMessageInput);
        tvStatusRestriction = findViewById(R.id.tvStatusRestriction);
        headerContent = findViewById(R.id.headerContent);

        if (receiverName != null) {
            tvTitle.setText(receiverName);
        }

        btnBack.setOnClickListener(v -> finish());

        adapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        checkChatPermissions();
        fetchUserName();
        listenForNewMessages();
        listenForReceiverPresence();
        loadInitialMessages();

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && lm.findFirstVisibleItemPosition() == 0 && !isLoadingMore && !allLoaded) {
                    loadMoreMessages();
                }
            }
        });

        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMessages.postDelayed(() -> {
                    if (!messageList.isEmpty()) rvMessages.scrollToPosition(messageList.size() - 1);
                }, 100);
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            
            if (header != null) {
                ViewGroup.LayoutParams lp = header.getLayoutParams();
                lp.height = (int)(75 * getResources().getDisplayMetrics().density) + systemBars.top;
                header.setLayoutParams(lp);
            }

            if (headerContent != null) {
                headerContent.setPadding(
                    headerContent.getPaddingLeft(),
                    systemBars.top + (int)(4 * getResources().getDisplayMetrics().density),
                    headerContent.getPaddingRight(),
                    headerContent.getPaddingBottom()
                );
            }
            
            View bottomArea = findViewById(R.id.bottomArea);
            if (bottomArea != null) {
                int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
                bottomArea.setPadding(
                    bottomArea.getPaddingLeft(),
                    bottomArea.getPaddingTop(),
                    bottomArea.getPaddingRight(),
                    bottomPadding + (int)(4 * getResources().getDisplayMetrics().density)
                );
            }
            
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /*
     * checkChatPermissions:
     * - Dùng để: kiểm tra quyền chat của hai bên.
     * - Kiểm tra/xử lý: đọc trạng thái làm việc của mình và của người nhận.
     * - Sau đó: nếu ai nghỉ việc thì khóa ô nhập và hiện thông báo.
     */
    private void checkChatPermissions() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(meDoc -> {
                    String myStatus = meDoc.getString("employmentStatus");
                    if (myStatus == null || myStatus.isEmpty()) {
                        myStatus = meDoc.getString("status");
                    }
                    String myRole = meDoc.getString("role");
                    
                    if (!isFinishing() && !isDestroyed() && header != null && "manager".equalsIgnoreCase(myRole)) {
                        header.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.amber_primary));
                    }

                    if ("Nghỉ việc".equals(myStatus)) {
                        disableChat("Bạn đã nghỉ việc, chỉ có thể xem lại tin nhắn cũ.");
                    } else {
                        db.collection("Users").document(receiverId).get()
                                .addOnSuccessListener(receiverDoc -> {
                                    String receiverStatus = receiverDoc.getString("employmentStatus");
                                    if (receiverStatus == null || receiverStatus.isEmpty()) {
                                        receiverStatus = receiverDoc.getString("status");
                                    }
                                    if ("Nghỉ việc".equals(receiverStatus)) {
                                        disableChat("Nhân viên này đã nghỉ việc, không thể gửi tin nhắn mới.");
                                    }
                                });
                    }
                });
    }

    /*
     * disableChat:
     * - Dùng để: ẩn vùng nhập tin nhắn khi bị hạn chế chat.
     * - Kiểm tra/xử lý: hide input và show thông báo lý do.
     * - Sau đó: màn hình chỉ còn đọc tin nhắn cũ.
     */
    private void disableChat(String message) {
        layoutMessageInput.setVisibility(View.GONE);
        tvStatusRestriction.setVisibility(View.VISIBLE);
        tvStatusRestriction.setText(message);
    }

    /*
     * fetchUserName:
     * - Dùng để: lấy tên hiển thị của người gửi.
     * - Kiểm tra/xử lý: đọc fullName từ document Users.
     * - Sau đó: dùng tên này khi lưu tin nhắn mới.
     */
    private void fetchUserName() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("fullName");
                    }
                });
    }

    /*
     * listenForReceiverPresence:
     * - Dùng để: nghe realtime trạng thái online/offline của người nhận.
     * - Kiểm tra/xử lý: đọc presenceStatus từ Firestore.
     * - Sau đó: cập nhật nhãn trạng thái ngay trên header chat.
     */
    private void listenForReceiverPresence() {
        presenceListener = db.collection("Users").document(receiverId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !value.exists()) return;
                    String status = value.getString("presenceStatus");
                    if (status == null || status.isEmpty()) {
                        status = value.getString("status");
                    }
                    
                    if ("online".equalsIgnoreCase(status)) {
                        tvReceiverPresence.setText("● Online");
                        tvReceiverPresence.setTextColor(android.graphics.Color.parseColor("#EAFBFF"));
                    } else {
                        tvReceiverPresence.setText("● Offline");
                        tvReceiverPresence.setTextColor(android.graphics.Color.parseColor("#B0BEC5"));
                    }
                });
    }

    /*
     * loadInitialMessages:
     * - Dùng để: tải lô tin nhắn đầu tiên khi mở chat.
     * - Kiểm tra/xử lý: lấy tin mới nhất, set mốc phân trang và đánh dấu đã xem.
     * - Sau đó: cuộn xuống tin cuối cùng trên màn hình.
     */
    private void loadInitialMessages() {
        String chatId = getChatId(currentUserId, receiverId);
        db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        oldestDocument = snap.getDocuments().get(snap.size() - 1);
                        if (snap.size() < PAGE_SIZE) allLoaded = true;
                    }
                    messageList.clear();
                    for (int i = snap.size() - 1; i >= 0; i--) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snap.getDocuments().get(i);
                        Message msg = doc.toObject(Message.class);
                        if (msg != null && msg.getSenderId() != null) {
                            msg.setMessageId(doc.getId());
                            messageList.add(msg);
                            if (!msg.isSeen() && !msg.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(doc.getId());
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) rvMessages.scrollToPosition(messageList.size() - 1);
                });
    }

    /*
     * loadMoreMessages:
     * - Dùng để: tải thêm tin nhắn cũ khi cuộn lên đầu.
     * - Kiểm tra/xử lý: dùng document cũ nhất làm mốc phân trang.
     * - Sau đó: chèn tin cũ vào đầu danh sách hiện tại.
     */
    private void loadMoreMessages() {
        if (oldestDocument == null || allLoaded) return;
        isLoadingMore = true;
        String chatId = getChatId(currentUserId, receiverId);
        db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(oldestDocument)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snap -> {
                    isLoadingMore = false;
                    if (snap.isEmpty()) { allLoaded = true; return; }
                    if (snap.size() < PAGE_SIZE) allLoaded = true;
                    oldestDocument = snap.getDocuments().get(snap.size() - 1);
                    int insertAt = 0;
                    for (int i = snap.size() - 1; i >= 0; i--) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snap.getDocuments().get(i);
                        Message msg = doc.toObject(Message.class);
                        if (msg != null && msg.getSenderId() != null) {
                            msg.setMessageId(doc.getId());
                            messageList.add(insertAt++, msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> isLoadingMore = false);
    }

    /*
     * listenForNewMessages:
     * - Dùng để: nghe realtime khi có tin nhắn mới.
     * - Kiểm tra/xử lý: tránh trùng messageId trước khi thêm vào list.
     * - Sau đó: tự động cập nhật UI và cuộn xuống cuối.
     */
    private void listenForNewMessages() {
        String chatId = getChatId(currentUserId, receiverId);
        messageListener = db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (QueryDocumentSnapshot doc : value) {
                        Message msg = doc.toObject(Message.class);
                        if (msg == null || msg.getSenderId() == null) continue;
                        msg.setMessageId(doc.getId());
                        boolean exists = false;
                        for (Message m : messageList) {
                            if (doc.getId().equals(m.getMessageId())) { exists = true; break; }
                        }
                        if (!exists) {
                            messageList.add(msg);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                        if (!msg.isSeen() && !msg.getSenderId().equals(currentUserId)) {
                            markMessageAsSeen(doc.getId());
                        }
                    }
                });
    }

    /*
     * onDestroy:
     * - Dùng để: dọn listener khi màn hình chat đóng.
     * - Kiểm tra/xử lý: remove listener của message và presence.
     * - Sau đó: tránh rò rỉ bộ nhớ và nghe realtime sai chỗ.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
        if (presenceListener != null) presenceListener.remove();
    }

    /*
     * markMessageAsSeen:
     * - Dùng để: cập nhật trạng thái đã xem cho tin nhắn.
     * - Kiểm tra/xử lý: update field seen trên document tương ứng.
     * - Sau đó: người gửi sẽ thấy tin nhắn đã được đọc.
     */
    private void markMessageAsSeen(String messageId) {
        String chatId = getChatId(currentUserId, receiverId);
        db.collection("Chats").document(chatId).collection("Messages").document(messageId)
                .update("seen", true);
    }

    /*
     * getChatId:
     * - Dùng để: tạo id phòng chat cố định cho 2 người.
     * - Kiểm tra/xử lý: sắp xếp uid theo alphabet để không bị đảo chiều.
     * - Sau đó: cả hai bên dùng chung một chatId.
     */
    private String getChatId(String id1, String id2) {
        if (id1.compareTo(id2) < 0) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }

    /*
     * sendMessage:
     * - Dùng để: gửi tin nhắn mới vào Firestore.
     * - Kiểm tra/xử lý: lưu message, cập nhật summary và gọi notification.
     * - Sau đó: tin nhắn hiện ngay trong phòng chat và người nhận được báo.
     */
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) || receiverId == null) return;

        Message message = new Message(currentUserId, currentUserName, text, System.currentTimeMillis());
        String chatId = getChatId(currentUserId, receiverId);
        Map<String, Object> chatSummary = new HashMap<>();
        chatSummary.put("participantIds", Arrays.asList(currentUserId, receiverId));
        chatSummary.put("lastMessage", text);
        chatSummary.put("lastMessageTime", message.getTimestamp());
        chatSummary.put("lastSenderId", currentUserId);
        chatSummary.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("Chats").document(chatId)
                .set(chatSummary, SetOptions.merge())
                .continueWithTask(task -> db.collection("Chats").document(chatId).collection("Messages").add(message))
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    NotificationHelper.sendToUser(
                            receiverId,
                            currentUserName != null ? currentUserName : "Tin nhắn mới",
                            text
                    );
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show());
    }

    /*
     * MessageAdapter:
     * - Dùng để: hiển thị tin nhắn theo 2 kiểu gửi và nhận.
     * - Kiểm tra/xử lý: xem senderId để chọn layout phù hợp.
     * - Sau đó: list chat nhìn rõ tin nào mình gửi, tin nào nhận.
     */
    private static class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_SENT = 1;
        private static final int TYPE_RECEIVED = 2;
        private List<Message> messages;
        private String currentUserId;

        public MessageAdapter(List<Message> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @Override
        public int getItemViewType(int position) {
            if (messages.get(position).getSenderId().equals(currentUserId)) {
                return TYPE_SENT;
            } else {
                return TYPE_RECEIVED;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_SENT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                return new SentViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
                return new ReceivedViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = messages.get(position);
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.getTimestamp()));
            
            if (holder instanceof SentViewHolder) {
                SentViewHolder vh = (SentViewHolder) holder;
                vh.tvMessage.setText(message.getMessage());
                vh.tvTime.setText(time);
                vh.tvStatus.setText(message.isSeen() ? "Đã xem" : "Đã gửi");
            } else {
                ReceivedViewHolder vh = (ReceivedViewHolder) holder;
                vh.tvSenderName.setText(message.getSenderName());
                vh.tvMessage.setText(message.getMessage());
                vh.tvTime.setText(time);
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        static class SentViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTime, tvStatus;
            SentViewHolder(View v) { 
                super(v); 
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTime = v.findViewById(R.id.tvTime);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }

        static class ReceivedViewHolder extends RecyclerView.ViewHolder {
            TextView tvSenderName, tvMessage, tvTime;
            ReceivedViewHolder(View v) {
                super(v);
                tvSenderName = v.findViewById(R.id.tvSenderName);
                tvMessage = v.findViewById(R.id.tvMessage);
                tvTime = v.findViewById(R.id.tvTime);
            }
        }
    }
}
