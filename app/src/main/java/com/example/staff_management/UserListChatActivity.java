package com.example.staff_management;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.ViewGroup;

import com.example.staff_management.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/*
 * UserListChatActivity:
 * - Dùng để: mở màn hình danh sách hội thoại chat.
 * - Kiểm tra/xử lý: lấy các cuộc trò chuyện hiện có, tìm kiếm và migrate chat cũ nếu cần.
 * - Sau đó: hiển thị danh sách chat mới nhất và cho phép mở chat mới.
 */
public class UserListChatActivity extends BaseActivity {

    private RecyclerView rvUserList;
    private EditText etSearch;
    private ImageButton btnBack;
    private FloatingActionButton fabNewMessage;
    private FirebaseFirestore db;
    private String currentUserId;
    private UserAdapter adapter;
    private final List<ChatItem> userList = new ArrayList<>();
    private final List<ChatItem> fullUserList = new ArrayList<>();
    private final Set<String> existingChatPartnerIds = new HashSet<>();
    private boolean legacyMigrationStarted = false;
    private ListenerRegistration chatListListener;
    private final List<ListenerRegistration> userListeners = new ArrayList<>();

    /*
     * onCreate:
     * - Dùng để: khởi tạo màn hình danh sách chat.
     * - Kiểm tra/xử lý: ánh xạ view, lấy dữ liệu user hiện tại và gắn bộ lọc tìm kiếm.
     * - Sau đó: load danh sách hội thoại và set bottom navigation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list_chat);

        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = requireAuthenticatedUser();
        if (currentUser == null) {
            return;
        }
        currentUserId = currentUser.getUid();

        rvUserList = findViewById(R.id.rvUserList);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        fabNewMessage = findViewById(R.id.fabNewMessage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.messengerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            
            View header = findViewById(R.id.header);
            if (header != null) {
                ViewGroup.LayoutParams lp = header.getLayoutParams();
                lp.height = (int)(75 * getResources().getDisplayMetrics().density) + systemBars.top;
                header.setLayoutParams(lp);
            }

            View headerContent = findViewById(R.id.headerContent);
            if (headerContent != null) {
                headerContent.setPadding(
                    headerContent.getPaddingLeft(),
                    systemBars.top + (int)(4 * getResources().getDisplayMetrics().density),
                    headerContent.getPaddingRight(),
                    headerContent.getPaddingBottom()
                );
            }
            
            View footerContainer = findViewById(R.id.footerContainer);
            if (footerContainer != null) {
                int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
                footerContainer.setPadding(
                    footerContainer.getPaddingLeft(),
                    footerContainer.getPaddingTop(),
                    footerContainer.getPaddingRight(),
                    bottomPadding + (int)(12 * getResources().getDisplayMetrics().density)
                );
            }
            
            if (fabNewMessage != null) {
                ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fabNewMessage.getLayoutParams();
                int bottomMargin = Math.max(systemBars.bottom, imeInsets.bottom);
                fabLp.bottomMargin = bottomMargin + (int)(26 * getResources().getDisplayMetrics().density);
                fabNewMessage.setLayoutParams(fabLp);
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        btnBack.setOnClickListener(v -> finish());

        setupBottomNavigation();

        adapter = new UserAdapter(userList);
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(adapter);

        loadConversationList();

        fabNewMessage.setOnClickListener(v -> showNewMessageDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /*
     * loadConversationList:
     * - Dùng để: lấy danh sách hội thoại đang có của user.
     * - Kiểm tra/xử lý: nghe realtime collection Chats theo participantIds.
     * - Sau đó: cập nhật list chat và sắp xếp theo tin nhắn mới nhất.
     */
    private void loadConversationList() {
        chatListListener = db.collection("Chats")
                .whereArrayContains("participantIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    List<ConversationSummary> summaries = new ArrayList<>();
                    existingChatPartnerIds.clear();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            List<String> participantIds = (List<String>) doc.get("participantIds");
                            Long lastMessageTime = doc.getLong("lastMessageTime");
                            String lastMessage = doc.getString("lastMessage");
                            if (participantIds == null || participantIds.isEmpty() || lastMessageTime == null || lastMessageTime <= 0) {
                                continue;
                            }

                            String otherUserId = getOtherParticipantId(participantIds);
                            if (otherUserId == null) {
                                continue;
                            }

                            existingChatPartnerIds.add(otherUserId);
                            summaries.add(new ConversationSummary(otherUserId, lastMessage, lastMessageTime));
                        }
                    }

                    summaries.sort((left, right) -> Long.compare(right.lastMessageTime, left.lastMessageTime));
                    populateConversationUsers(summaries);

                    if (!legacyMigrationStarted) {
                        legacyMigrationStarted = true;
                        migrateLegacyChats();
                    }
                });
    }

    /*
     * onDestroy:
     * - Dùng để: dọn listener khi màn hình chat bị đóng.
     * - Kiểm tra/xử lý: remove listener của danh sách chat và listener user con.
     * - Sau đó: tránh leak và tránh nghe dữ liệu nền không cần thiết.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListListener != null) chatListListener.remove();
        for (ListenerRegistration lr : userListeners) lr.remove();
        userListeners.clear();
    }

    /*
     * getOtherParticipantId:
     * - Dùng để: lấy userId của người chat bên kia.
     * - Kiểm tra/xử lý: duyệt danh sách participant rồi bỏ qua chính currentUserId.
     * - Sau đó: biết chính xác chat này đang nói chuyện với ai.
     */
    private String getOtherParticipantId(List<String> participantIds) {
        for (String participantId : participantIds) {
            if (participantId != null && !participantId.equals(currentUserId)) {
                return participantId;
            }
        }
        return null;
    }

    /*
     * populateConversationUsers:
     * - Dùng để: lấy thông tin user cho từng cuộc trò chuyện.
     * - Kiểm tra/xử lý: lắng nghe realtime từng document Users và ghép với tin nhắn cuối.
     * - Sau đó: danh sách chat hiển thị tên, avatar và trạng thái mới nhất.
     */
    private void populateConversationUsers(List<ConversationSummary> summaries) {
        for (ListenerRegistration lr : userListeners) lr.remove();
        userListeners.clear();

        if (summaries.isEmpty()) {
            fullUserList.clear();
            filter(etSearch.getText().toString());
            return;
        }

        List<ChatItem> loadedItems = new ArrayList<>(Collections.nCopies(summaries.size(), null));
        final int[] pending = {summaries.size()};

        for (int index = 0; index < summaries.size(); index++) {
            int targetIndex = index;
            ConversationSummary summary = summaries.get(index);
            
            ListenerRegistration lr = db.collection("Users").document(summary.otherUserId)
                    .addSnapshotListener((userDoc, error) -> {
                        if (userDoc != null && userDoc.exists()) {
                            User user = userDoc.toObject(User.class);
                            if (user != null) {
                                user.setUid(summary.otherUserId);
                                ChatItem newItem = new ChatItem(user, summary.lastMessage, summary.lastMessageTime);
                                
                                boolean updated = false;
                                for (int i = 0; i < fullUserList.size(); i++) {
                                    if (fullUserList.get(i).user.getUid().equals(summary.otherUserId)) {
                                        fullUserList.set(i, newItem);
                                        updated = true;
                                        break;
                                    }
                                }
                                if (updated) {
                                    filter(etSearch.getText().toString());
                                }
                                loadedItems.set(targetIndex, newItem);
                            }
                        }
                        
                        if (pending[0] > 0) {
                            pending[0]--;
                            if (pending[0] == 0) {
                                fullUserList.clear();
                                for (ChatItem item : loadedItems) {
                                    if (item != null) fullUserList.add(item);
                                }
                                filter(etSearch.getText().toString());
                            }
                        }
                    });
            userListeners.add(lr);
        }
    }

    /*
     * migrateLegacyChats:
     * - Dùng để: chuyển chat cũ sang cấu trúc mới.
     * - Kiểm tra/xử lý: dò các user chưa có summary document trong Chats.
     * - Sau đó: tạo lại dữ liệu hội thoại để màn chat đọc đồng nhất.
     */
    private void migrateLegacyChats() {
        db.collection("Users").get().addOnSuccessListener(snapshot -> {
            for (QueryDocumentSnapshot doc : snapshot) {
                User user = doc.toObject(User.class);
                if (user == null) {
                    continue;
                }

                String otherUserId = user.getUid();
                if (otherUserId == null || otherUserId.isEmpty()) {
                    otherUserId = doc.getId();
                    user.setUid(otherUserId);
                }

                if (otherUserId == null || otherUserId.equals(currentUserId) || existingChatPartnerIds.contains(otherUserId)) {
                    continue;
                }

                migrateLegacyChatForUser(user);
            }
        });
    }

    /*
     * migrateLegacyChatForUser:
     * - Dùng để: tạo summary cho một cuộc chat cũ với một user.
     * - Kiểm tra/xử lý: lấy tin nhắn mới nhất trong Messages rồi gom vào document Chats.
     * - Sau đó: conversation cũ có thể hiển thị lại trên danh sách chat.
     */
    private void migrateLegacyChatForUser(User user) {
        String otherUserId = user.getUid();
        String chatId = getChatId(currentUserId, otherUserId);
        db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(messageSnapshot -> {
                    if (messageSnapshot.isEmpty()) {
                        return;
                    }

                    DocumentSnapshot latestMessage = messageSnapshot.getDocuments().get(0);
                    Long lastMessageTime = latestMessage.getLong("timestamp");
                    String lastMessage = latestMessage.getString("message");
                    if (lastMessageTime == null || lastMessage == null || lastMessage.isEmpty()) {
                        return;
                    }

                    HashMap<String, Object> chatSummary = new HashMap<>();
                    chatSummary.put("participantIds", Arrays.asList(currentUserId, otherUserId));
                    chatSummary.put("lastMessage", lastMessage);
                    chatSummary.put("lastMessageTime", lastMessageTime);
                    chatSummary.put("lastSenderId", latestMessage.getString("senderId"));
                    chatSummary.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("Chats").document(chatId)
                            .set(chatSummary, SetOptions.merge());
                });
    }

    /*
     * getChatId:
     * - Dùng để: tạo chatId duy nhất cho 2 người dùng.
     * - Kiểm tra/xử lý: sắp xếp 2 userId theo thứ tự alphabet trước khi ghép chuỗi.
     * - Sau đó: chat của hai người luôn có cùng một id dù mở từ phía nào.
     */
    private String getChatId(String id1, String id2) {
        if (id1.compareTo(id2) < 0) {
            return id1 + "_" + id2;
        }
        return id2 + "_" + id1;
    }

    /*
     * showNewMessageDialog:
     * - Dùng để: mở dialog chọn người dùng mới để nhắn tin.
     * - Kiểm tra/xử lý: loại trừ người đã có chat và chính tài khoản hiện tại.
     * - Sau đó: người dùng chọn một người để chuyển sang màn chat.
     */
    private void showNewMessageDialog() {
        db.collection("Users").get().addOnSuccessListener(snapshot -> {
            List<User> availableUsers = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                User user = doc.toObject(User.class);
                if (user == null) {
                    continue;
                }

                String userId = user.getUid();
                if (userId == null || userId.isEmpty()) {
                    userId = doc.getId();
                    user.setUid(userId);
                }

                if (userId == null || userId.equals(currentUserId) || existingChatPartnerIds.contains(userId)) {
                    continue;
                }

                availableUsers.add(user);
            }

            availableUsers.sort(Comparator.comparing(user -> {
                String fullName = user.getFullName();
                return fullName != null ? fullName.toLowerCase(Locale.getDefault()) : "";
            }));

            if (availableUsers.isEmpty()) {
                Toast.makeText(this, "Khong con nguoi dung moi de tao tin nhan", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[availableUsers.size()];
            for (int i = 0; i < availableUsers.size(); i++) {
                names[i] = availableUsers.get(i).getFullName();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Tao tin nhan moi")
                    .setItems(names, (dialog, which) -> openChat(availableUsers.get(which)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    /*
     * setupBottomNavigation:
     * - Dùng để: cấu hình thanh điều hướng dưới theo role.
     * - Kiểm tra/xử lý: đổi menu cho admin, manager hoặc employee rồi bắt sự kiện từng tab.
     * - Sau đó: user chuyển màn hình đúng luồng mà hệ thống đang hỗ trợ.
     */
    private void setupBottomNavigation() {
        db.collection("Users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            String role = documentSnapshot.getString("role");
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
            View header = findViewById(R.id.header);
            TextView tvTitle = findViewById(R.id.tvTitle);

            if ("admin".equals(role)) {
                bottomNavigation.getMenu().clear();
                bottomNavigation.inflateMenu(R.menu.admin_bottom_nav_menu);
            } else if ("manager".equals(role)) {
                bottomNavigation.getMenu().clear();
                bottomNavigation.inflateMenu(R.menu.manager_bottom_nav_menu);
                int blue = androidx.core.content.ContextCompat.getColor(this, R.color.blue_primary);
                int white = androidx.core.content.ContextCompat.getColor(this, R.color.white);
                if (!isFinishing() && !isDestroyed()) {
                    if (header != null) header.setBackgroundColor(blue);
                    if (tvTitle != null) tvTitle.setTextColor(white);
                    if (btnBack != null) btnBack.setColorFilter(white);
                    if (fabNewMessage != null) fabNewMessage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
                }
            } else {
                bottomNavigation.getMenu().clear();
                bottomNavigation.inflateMenu(R.menu.employee_bottom_nav_menu);
                if (fabNewMessage != null) {
                    fabNewMessage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.blue_primary)));
                }
            }

            bottomNavigation.setSelectedItemId(R.id.nav_messenger);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    finish();
                    return true;
                } else if (id == R.id.nav_messenger) {
                    return true;
                } else if (id == R.id.nav_stats) {
                    startActivity(new Intent(this, StatisticsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_task || id == R.id.nav_my_team) {
                    db.collection("Users").document(currentUserId).get().addOnSuccessListener(doc -> {
                        String currentRole = doc.getString("role");
                        Intent intent;
                        if (id == R.id.nav_task) {
                            if ("manager".equalsIgnoreCase(currentRole)) {
                                intent = new Intent(this, ManagerTaskActivity.class);
                            } else {
                                intent = new Intent(this, TaskActivity.class);
                            }
                        } else {
                            intent = new Intent(this, EmployeeListActivity.class);
                            intent.putExtra("mode", "productivity");
                            if ("manager".equalsIgnoreCase(currentRole)) {
                                intent.putExtra("role", "manager");
                            }
                        }
                        startActivity(intent);
                        finish();
                    });
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    showAiAssistantDialog();
                    return true;
                }
                return false;
            });
        });
    }

    /*
     * filter:
     * - Dùng để: lọc danh sách hội thoại theo từ khóa tìm kiếm.
     * - Kiểm tra/xử lý: so tên user với chuỗi nhập vào trên ô search.
     * - Sau đó: RecyclerView chỉ còn các cuộc chat khớp từ khóa.
     */
    private void filter(String query) {
        userList.clear();
        if (TextUtils.isEmpty(query)) {
            userList.addAll(fullUserList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault()).trim();
            for (ChatItem item : fullUserList) {
                String fullName = item.user.getFullName();
                if (fullName != null && fullName.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    userList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    /*
     * openChat:
     * - Dùng để: mở màn hình chat chi tiết với một người dùng.
     * - Kiểm tra/xử lý: truyền receiverId và receiverName qua Intent.
     * - Sau đó: chuyển sang ChatRoomActivity để nhắn tin trực tiếp.
     */
    private void openChat(User user) {
        Intent intent = new Intent(UserListChatActivity.this, ChatRoomActivity.class);
        intent.putExtra("receiverId", user.getUid());
        intent.putExtra("receiverName", user.getFullName());
        startActivity(intent);
    }

    /*
     * ConversationSummary:
     * - Dùng để: lưu dữ liệu tóm tắt của một hội thoại.
     * - Kiểm tra/xử lý: giữ userId đối phương, tin nhắn cuối và thời gian tin nhắn.
     * - Sau đó: danh sách chat dùng dữ liệu này để sắp xếp và hiển thị.
     */
    private static class ConversationSummary {
        final String otherUserId;
        final String lastMessage;
        final long lastMessageTime;

        ConversationSummary(String otherUserId, String lastMessage, long lastMessageTime) {
            this.otherUserId = otherUserId;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
        }
    }

    /*
     * ChatItem:
     * - Dùng để: ghép thông tin user với tin nhắn cuối của hội thoại.
     * - Kiểm tra/xử lý: giữ user, lastMessage và lastMessageTime trong cùng một object.
     * - Sau đó: adapter dùng để vẽ từng dòng chat.
     */
    private static class ChatItem {
        final User user;
        final String lastMessage;
        final long lastMessageTime;

        ChatItem(User user, String lastMessage, long lastMessageTime) {
            this.user = user;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
        }
    }

    /*
     * UserAdapter:
     * - Dùng để: hiển thị danh sách hội thoại trên RecyclerView.
     * - Kiểm tra/xử lý: bind avatar, tên, tin nhắn cuối, thời gian và trạng thái online.
     * - Sau đó: người dùng nhìn thấy danh sách chat gọn và dễ bấm vào từng người.
     */
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<ChatItem> users;

        UserAdapter(List<ChatItem> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_chat, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            ChatItem item = users.get(position);
            User user = item.user;

            holder.tvUserName.setText(user.getFullName());
            holder.tvLastMessage.setText(item.lastMessage != null && !item.lastMessage.isEmpty() ? item.lastMessage : "");
            holder.tvUnreadCount.setVisibility(View.GONE);

            if (item.lastMessageTime > 0) {
                String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(item.lastMessageTime));
                holder.tvLastMessageTime.setText(timeStr);
            } else {
                holder.tvLastMessageTime.setText("");
            }

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(user.getAvatarUrl())
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

            if ("online".equalsIgnoreCase(user.getPresenceStatus())) {
                holder.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GREEN));
            } else {
                holder.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            }

            holder.itemView.setOnClickListener(v -> openChat(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvLastMessage, tvLastMessageTime, tvUnreadCount;
            View statusIndicator;
            ShapeableImageView ivUserAvatar;

            UserViewHolder(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                tvLastMessage = v.findViewById(R.id.tvLastMessage);
                tvLastMessageTime = v.findViewById(R.id.tvLastMessageTime);
                tvUnreadCount = v.findViewById(R.id.tvUnreadCount);
                statusIndicator = v.findViewById(R.id.statusIndicator);
                ivUserAvatar = v.findViewById(R.id.ivUserAvatar);
            }
        }
    }
}
