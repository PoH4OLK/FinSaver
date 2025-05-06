package com.example.finsaver.settings;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finsaver.models.Users;
import com.example.finsaver.utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateProfileViewModel extends ViewModel {

    private final MutableLiveData<Boolean> updateUserProfileStatus = new MutableLiveData<>();

    public LiveData<Boolean> changeProfile(int userId, Users user){
        new Thread(() -> {
           try (Connection conn = DatabaseHelper.getConnection()) {
               String sql = "UPDATE Users SET Username = ?, Password = ?, Email = ?, FullName = ? WHERE UserID = ?";
               PreparedStatement stmt = conn.prepareStatement(sql);

               stmt.setString(1, user.getUsername());
               stmt.setString(2, user.getPassword());
               stmt.setString(3, user.getEmail());
               stmt.setString(4, user.getFullName());
               stmt.setInt(5, userId);

               int affectRows = stmt.executeUpdate();
               boolean success = affectRows > 0;
               updateUserProfileStatus.postValue(success);

           } catch (SQLException e){
               Log.e("UpdateProfileViewModel", "Ошибка БД", e);
               updateUserProfileStatus.postValue(false);
           }
        }).start();

        return updateUserProfileStatus;
    }

    public void clearUpdateStatus() {
        updateUserProfileStatus.setValue(null);
    }

    public LiveData<Boolean> getUpdateUserProfileStatus(){
        return updateUserProfileStatus;
    }
}
