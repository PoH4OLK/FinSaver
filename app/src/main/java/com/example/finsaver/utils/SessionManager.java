package com.example.finsaver.utils;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.finsaver.models.Users;

public class SessionManager {
    private static final String PREF_NAME = "Finance saver";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_FULLNAME = "fullName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_DARK_MODE = "dark_mode";
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Сохраняет данные пользователя при входе
     */
    public void createLoginSession(Users user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_PASSWORD, user.getPassword());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_FULLNAME, user.getFullName());
        editor.apply();

        Log.d("Session", "Создана сессия для пользователя: " + user.getUsername());
    }

    /**
     * Сохраняет данные для "Запомнить меня"
     */
    public void setRememberMe(boolean remember, Users user) {
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        if (remember) {
            editor.putString(KEY_USERNAME, user.getUsername());
            editor.putString(KEY_PASSWORD, user.getPassword());
        } else {
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
        }
        editor.apply();
    }

    /**
     * Проверяет, включено ли "Запомнить меня"
     */
    public boolean isRememberMe() {
        return pref.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * Выход пользователя
     */
    public void logoutUser(){
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_FULLNAME);
        editor.clear();
        editor.apply();
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    public String getFullName() {
        return pref.getString(KEY_FULLNAME, null);
    }

    public String getPassword() {
        return pref.getString(KEY_PASSWORD, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    /**
     * Получает ID пользователя
     */
    public int getUserId() {
        if (pref == null) {
            Log.e(TAG, "SharedPreferences не инициализирован!");
            return -1;
        }
        return pref.getInt(KEY_USER_ID, -1);
    }
    public Users getCurrentUser() {
        Users user = new Users();
        user.setUserId(getUserId());
        user.setUsername(getUsername());
        user.setPassword(getPassword());
        user.setEmail(getEmail());
        user.setFullName(getFullName());
        return user;
    }

    /**
     * Проверяет, выполнен ли вход
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public boolean isDarkModeEnabled() {
        return pref.getBoolean(KEY_DARK_MODE, false);
    }
}
