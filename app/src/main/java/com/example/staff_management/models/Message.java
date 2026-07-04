package com.example.staff_management.models;

/*
 * Message:
 * - Dùng để: lưu một tin nhắn trong phòng chat.
 * - Kiểm tra/xử lý: map với collection Messages trên Firestore và theo dõi trạng thái seen.
 * - Sau đó: màn chat dùng dữ liệu này để hiển thị nội dung, giờ gửi và trạng thái đã xem.
 */
public class Message {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;
    private boolean seen;

    public Message() {}

    public Message(String senderId, String senderName, String message, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
}
