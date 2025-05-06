package com.example.finsaver.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finsaver.models.Users;
import com.example.finsaver.utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginViewModel extends ViewModel {
    private final MutableLiveData<Boolean> loginResult = new MutableLiveData<>();
    private int currentUserId = -1;
    private String currentFullName;
    private String currentEmail;
    private String currentUsername;
    private String currentPassword;

    public LiveData<Boolean> login(String username, String password) {

        loginResult.setValue(null);

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "SELECT UserID, Username, Password, Email, FullName FROM Users WHERE Username = ? AND Password = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("UserID");
                    currentUsername = rs.getString("Username");
                    currentPassword = rs.getString("Password");
                    currentEmail = rs.getString("Email");
                    currentFullName = rs.getString("FullName");
                    loginResult.postValue(true);
                } else {
                    loginResult.postValue(false);
                }
            } catch (SQLException e) {
                loginResult.postValue(false);
            }
        }).start();

        return loginResult;
    }

    public Users getCurrentUser() {
        // Возвращаем полностью заполненный объект Users
        Users user = new Users();
        user.setUserId(currentUserId);
        user.setUsername(currentUsername);
        user.setPassword(currentPassword);
        user.setEmail(currentEmail);
        user.setFullName(currentFullName);
        return user;
    }
}
