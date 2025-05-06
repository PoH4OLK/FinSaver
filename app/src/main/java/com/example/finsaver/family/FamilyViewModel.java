package com.example.finsaver.family;

import static android.content.ContentValues.TAG;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finsaver.models.FamilyGroup;
import com.example.finsaver.utils.DatabaseHelper;
import com.example.finsaver.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FamilyViewModel extends ViewModel {
    private final MutableLiveData<Boolean> requestResult = new MutableLiveData<>();
    private final MutableLiveData<List<FamilyGroup>> familyMembers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> invitationAccepted = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isGroupCreator = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> requireUiUpdate = new MutableLiveData<>(false);


    private int currentUserId = -1;

    public void init(int userId) {
        this.currentUserId = userId;
        loadFamilyMembers();
    }

    public LiveData<List<FamilyGroup>> getFamilyMembers() {
        return familyMembers;
    }

    private boolean isUserInGroup(Connection conn, int userId, int groupId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM UserFamily WHERE UserID = ? AND GroupID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public LiveData<Boolean> getInvitationAccepted() {
        return invitationAccepted;
    }

    public void loadFamilyMembers() {
        if (currentUserId == -1) {
            Log.e(TAG, "Ошибка: currentUserId не установлен");
            return;
        }

        Log.d(TAG, "Начало загрузки данных группы для userID: " + currentUserId);

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    Log.e(TAG, "Нет соединения с БД");
                    return;
                }
                String sql = "SELECT u.UserID, uf.GroupID, u.Username, u.FullName, " +
                        "COALESCE(SUM(CASE WHEN t.TransactionType = 'income' THEN t.Amount ELSE 0 END), 0) as TotalIncome, " +
                        "COALESCE(SUM(CASE WHEN t.TransactionType = 'expense' THEN t.Amount ELSE 0 END), 0) as TotalExpenses, " +
                        "CASE WHEN fg.CreatorID = uf.UserID THEN 1 ELSE 0 END as IsCreator " +
                        "FROM UserFamily uf " +
                        "JOIN Users u ON uf.UserID = u.UserID " +
                        "JOIN FamilyGroups fg ON uf.GroupID = fg.GroupID " +
                        "LEFT JOIN Transactions t ON u.UserID = t.UserID " +
                        "WHERE uf.GroupID IN (SELECT GroupID FROM UserFamily WHERE UserID = ?) " +
                        "AND uf.IsApproved = 1 " +
                        "GROUP BY u.UserID, uf.GroupID, u.Username, u.FullName, fg.CreatorID, uf.UserID";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, currentUserId);

                ResultSet rs = stmt.executeQuery();
                List<FamilyGroup> members = new ArrayList<>();
                boolean creatorFound = false;

                while (rs.next()) {
                    boolean isCreator = rs.getBoolean("IsCreator");
                    if (rs.getInt("UserID") == currentUserId && isCreator) {
                        creatorFound = true;
                    }
                    FamilyGroup member = new FamilyGroup(
                            rs.getInt("UserID"),
                            rs.getString("Username"),
                            rs.getString("FullName"),
                            rs.getDouble("TotalIncome"),
                            rs.getDouble("TotalExpenses"),
                            isCreator
                    );
                    member.setGroupId(rs.getInt("GroupID"));
                    members.add(member);

                    Log.d(TAG, "Загружен участник: " + member.getName() +
                            ", доход: " + member.getTotalIncome() +
                            ", расход: " + member.getTotalExpenses());
                }

                familyMembers.postValue(members);
                isGroupCreator.postValue(creatorFound);
                Log.d(TAG, "Данные группы успешно загружены. Участников: " + members.size());
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка загрузки данных группы", e);
                familyMembers.postValue(new ArrayList<>());
                isGroupCreator.postValue(false);
            }
        }).start();
    }

    // Добавляем метод для создания группы
    public LiveData<Boolean> createFamilyGroup() {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "INSERT INTO FamilyGroups (GroupName, CreatorID) VALUES ('Family', ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, RETURN_GENERATED_KEYS);
                stmt.setInt(1, currentUserId);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int groupId = keys.getInt(1);

                    // Добавляем создателя в группу
                    String addUserSql = "INSERT INTO UserFamily (UserID, GroupID, IsApproved) VALUES (?, ?, 1)";
                    PreparedStatement addUserStmt = conn.prepareStatement(addUserSql);
                    addUserStmt.setInt(1, currentUserId);
                    addUserStmt.setInt(2, groupId);
                    addUserStmt.executeUpdate();

                    result.postValue(true);
                    loadFamilyMembers();
                } else {
                    result.postValue(false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                result.postValue(false);
            }
        }).start();

        return result;
    }

    // Добавляем метод для расформирования группы
    public LiveData<Boolean> disbandGroup(int groupId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        new Thread(() -> {
            Connection conn = null;
            try {
                conn = DatabaseHelper.getConnection();
                if (conn == null) {
                    result.postValue(false);
                    return;
                }

                // Начинаем транзакцию
                conn.setAutoCommit(false);

                // Удаляем всех участников группы
                String deleteMembersSql = "DELETE FROM UserFamily WHERE GroupID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                    stmt.setInt(1, groupId);
                    stmt.executeUpdate();
                }

                // Удаляем саму группу
                String deleteGroupSql = "DELETE FROM FamilyGroups WHERE GroupID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteGroupSql)) {
                    stmt.setInt(1, groupId);
                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        conn.commit();
                        // Обновляем состояние
                        isGroupCreator.postValue(false);
                        familyMembers.postValue(Collections.emptyList());
                        result.postValue(true);
                    } else {
                        conn.rollback();
                        result.postValue(false);
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при расформировании группы", e);
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    Log.e(TAG, "Ошибка при откате транзакции", ex);
                }
                result.postValue(false);
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException e) {
                    Log.e(TAG, "Ошибка при закрытии соединения", e);
                }
            }
        }).start();

        return result;
    }

    public LiveData<Boolean> getIsGroupCreator() {
        return isGroupCreator;
    }

    public LiveData<Boolean> sendFamilyRequest(String username, String message) {

        requestResult.setValue(null);

        if (currentUserId == -1 || username == null) {
            requestResult.postValue(false);
            return requestResult;
        }

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                // Проверяем существование пользователя
                String checkUserSql = "SELECT UserID FROM Users WHERE Username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkUserSql);
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();

                if(rs.next()){
                    int receiverId = rs.getInt("UserID");
                    int groupId = getOrCreateFamilyGroup(conn);

                    // Проверяем существование польльхователя в группе
                    String checkMemberInGroupSql ="SELECT COUNT(*) FROM UserFamily WHERE UserID =? AND GroupID = ? AND IsApproved = 1";
                    PreparedStatement checkMemberInGroupStmp = conn.prepareStatement(checkMemberInGroupSql);
                    checkMemberInGroupStmp.setInt(1, receiverId);
                    checkMemberInGroupStmp.setInt(2, groupId);
                    ResultSet memberRs = checkMemberInGroupStmp.executeQuery();

                    if(memberRs.next() && memberRs.getInt(1) > 0){
                        requestResult.postValue(false);
                        return;
                    }

                    // Отправляем запрос
                    String insertSql = "INSERT INTO Notifications (SenderID, ReceiverID, GroupID, Message) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, currentUserId);
                    insertStmt.setInt(2, receiverId);
                    insertStmt.setInt(3, groupId);
                    insertStmt.setString(4, message);

                    int affectedRows = insertStmt.executeUpdate();
                    requestResult.postValue(affectedRows > 0);
                } else {
                    requestResult.postValue(false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requestResult.postValue(false);
            }
        }).start();

        return requestResult;
    }

    public LiveData<Boolean> removeFamilyMember(int userId, int groupId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                // Проверяем права текущего пользователя
                String checkRightsSql = "SELECT CreatorID FROM FamilyGroups WHERE GroupID = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkRightsSql);
                checkStmt.setInt(1, groupId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    int creatorId = rs.getInt("CreatorID");

                    // Проверяем, может ли пользователь удалять:
                    // Создатель может удалять любого (кроме себя)
                    // Обычный пользователь может удалять только себя
                    if ((creatorId == currentUserId && userId != currentUserId) ||
                            (creatorId != currentUserId && userId == currentUserId)) {

                        String deleteSql = "DELETE FROM UserFamily WHERE UserID = ? AND GroupID = ?";
                        PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                        deleteStmt.setInt(1, userId);
                        deleteStmt.setInt(2, groupId);

                        int affectedRows = deleteStmt.executeUpdate();
                        boolean success = affectedRows > 0;
                        result.postValue(success);

                        // Всегда обновляем список участников
                        loadFamilyMembers();

                        // Если пользователь удалил себя - сбрасываем статус создателя
                        if (success && userId == currentUserId) {
                            isGroupCreator.postValue(false);
                        }
                    } else {
                        result.postValue(false);
                        Log.e(TAG, "Недостаточно прав для удаления участника");
                    }
                } else {
                    result.postValue(false);
                    Log.e(TAG, "Группа не найдена");
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка удаления участника", e);
                result.postValue(false);
            }
        }).start();

        return result;
    }

    public void acceptInvitation(int groupId, int userId) {
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                // Проверяем, не состоит ли уже пользователь в группе
                if (isUserInGroup(conn, userId, groupId)) {
                    return;
                }

                // Добавляем пользователя в группу
                addUserToGroup(conn, userId, groupId);

                // Помечаем уведомление как принятое
                new DatabaseHelper().updateNotificationStatus(groupId, userId, "accepted");

                // Обновляем список участников
                loadFamilyMembers();

            } catch (SQLException e) {
                Log.e(TAG, "Ошибка принятия приглашения", e);
            }
        }).start();
    }

    private int getOrCreateFamilyGroup(Connection conn) throws SQLException {
        // Проверяем существующую группу
        String checkSql = "SELECT GroupID FROM UserFamily WHERE UserID = ? AND IsApproved = 1";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setInt(1, currentUserId);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("GroupID");
        }

        // Создаем новую группу
        String createSql = "INSERT INTO FamilyGroups (GroupName) VALUES ('Family')";
        PreparedStatement createStmt = conn.prepareStatement(createSql, RETURN_GENERATED_KEYS);
        createStmt.executeUpdate();

        ResultSet keys = createStmt.getGeneratedKeys();
        if (keys.next()) {
            int groupId = keys.getInt(1);

            // Добавляем пользователя в группу
            String addUserSql = "INSERT INTO UserFamily (UserID, GroupID, IsApproved) VALUES (?, ?, 1)";
            PreparedStatement addUserStmt = conn.prepareStatement(addUserSql);
            addUserStmt.setInt(1, currentUserId);
            addUserStmt.setInt(2, groupId);
            addUserStmt.executeUpdate();

            return groupId;
        }

        throw new SQLException("Не удалось создать группу");
    }

    public void declineInvitation(int groupId, int userId) {
        new Thread(() -> {
            try {
                new DatabaseHelper().updateNotificationStatus(groupId, userId, "declined");
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при отклонении приглашения", e);
            }
        }).start();
    }

    private void addUserToGroup(Connection conn, int userId, int groupId) throws SQLException {
        String sql = "INSERT INTO UserFamily (UserID, GroupID, IsApproved) VALUES (?, ?, 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Не удалось добавить пользователя в группу");
            }
        }
    }

    public void setRequireUiUpdate(boolean required) {
        requireUiUpdate.postValue(required);
    }
}
