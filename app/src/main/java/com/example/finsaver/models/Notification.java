package com.example.finsaver.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Notification {
    private String id;
    private String senderName;
    private String groupId;
    private String groupName;
    private String message;
    private String status;
    private long timestamp;
    private boolean isRead;

    // Конструктор
    public Notification(String id, String senderName, String groupId,
                        String groupName, String message, long timestamp,
                        boolean isRead, String staus) {
        this.id = id;
        this.senderName = senderName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.status = staus;
    }

    // Геттеры
    public String getId() { return id; }
    public String getSenderName() { return senderName; }
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getFormattedTime() {
        return new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }
}