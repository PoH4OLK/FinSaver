package com.example.finsaver.auth;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finsaver.models.Users;
import com.example.finsaver.utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<Boolean> registrationResult = new MutableLiveData<>();

    public LiveData<Boolean> register(Users user) {
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "INSERT INTO Users (Username, Password, Email, FullName) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);

                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.setString(3, user.getEmail());
                stmt.setString(4, user.getFullName());

                int affectedRows = stmt.executeUpdate();
                boolean success = affectedRows > 0;

                registrationResult.postValue(success);
            } catch (SQLException e) {
                Log.e("RegisterViewModel", "Ошибка БД", e);
                registrationResult.postValue(false);
            }
        }).start();

        return registrationResult;
    }
}
