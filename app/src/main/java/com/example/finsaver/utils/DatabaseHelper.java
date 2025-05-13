package com.example.finsaver.utils;

import com.example.finsaver.models.Notification;
import com.example.finsaver.models.Transactions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String IP = "";
    private static final String JDBC_URL = "jdbc:jtds:sqlserver://" + IP + ":1433/FinSaverDB";
    private static final String USER = "";
    private static final String PASS = "";

    static {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASS);

    }

    // Метод для получения списка уведомлений
    public List<Notification> getNotifications(int userId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT n.NotificationID, u.Username AS SenderName, " +
                "n.GroupID, g.GroupName, n.Message, n.CreatedAt, n.IsRead, n.Status " +
                "FROM Notifications n " +
                "JOIN Users u ON n.SenderID = u.UserID " +
                "LEFT JOIN FamilyGroups g ON n.GroupID = g.GroupID " +
                "WHERE n.ReceiverID = ? " +
                "ORDER BY n.CreatedAt DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                notifications.add(new Notification(
                        String.valueOf(rs.getInt("NotificationID")),
                        rs.getString("SenderName"),
                        String.valueOf(rs.getInt("GroupID")),
                        rs.getString("GroupName"),
                        rs.getString("Message"),
                        rs.getTimestamp("CreatedAt").getTime(),
                        rs.getBoolean("IsRead"),
                        rs.getString("Status")
                ));
            }
        }
        return notifications;
    }

    public void updateNotificationStatus(int groupId, int userId, String status) throws SQLException {
        String sql = "UPDATE Notifications SET IsRead = 1, Status = ? WHERE GroupID = ? AND ReceiverID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, groupId);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        }
    }
}
