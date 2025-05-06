package com.example.finsaver.settings;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.finsaver.AboutActivity;
import com.example.finsaver.R;
import com.example.finsaver.auth.LoginActivity;
import com.example.finsaver.models.Users;
import com.example.finsaver.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileSettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private static final int UPDATE_PROFILE_REQUEST = 1;
    private SwitchMaterial themeSwitch;
    private TextView tvUsername, tvFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        sessionManager = new SessionManager(this);
        themeSwitch = findViewById(R.id.switchTheme);

        // Установка начального состояния переключателя
        themeSwitch.setChecked(sessionManager.isDarkModeEnabled());

        // Обработка переключения темы
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkModeEnabled(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate(); // Пересоздаем активити для применения темы
        });

        tvUsername = findViewById(R.id.tvUsername);
        tvFullName = findViewById(R.id.tvFullName);
    }

    private void setupButtons() {
        try {
            View btnAbout = findViewById(R.id.btnAbout);
            if (btnAbout != null) {
                btnAbout.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка О программе");
                    redirectToAbout();
                });
            }
            View btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
            if(btnUpdateProfile != null){
                btnUpdateProfile.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка Изменить профиль");
                    redirectToUpdateProfile();
                });
            }
            View btnLogout = findViewById(R.id.btnLogout);
            btnLogout.setOnClickListener(v -> logoutUser());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке кнопок", e);
        }


    }

    private void infoAboutUser(){
        tvUsername.setText(sessionManager.getUsername());
        tvFullName.setText(sessionManager.getFullName());
    }

    private void updateInfoAboutUser(String userName, String FullName) {
        if (tvUsername != null && tvFullName != null) {
            tvUsername.setText(userName);
            tvFullName.setText(FullName);
        }
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    sessionManager.logoutUser();
                    redirectToLogin();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void redirectToUpdateProfile() {
        Users user = sessionManager.getCurrentUser();
        Intent intent = new Intent(this, UpdateProfileActivity.class);
        intent.putExtra("user", user);
        startActivityForResult(intent, UPDATE_PROFILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPDATE_PROFILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("updatedUser")) {
                Users updatedUser = (Users) data.getSerializableExtra("updatedUser");

                // Обновляем сессию
                sessionManager.createLoginSession(updatedUser);

                // Обновляем UI
                TextView tvUsername = findViewById(R.id.tvUsername);
                TextView tvFullName = findViewById(R.id.tvFullName);

                if (tvUsername != null) tvUsername.setText(updatedUser.getUsername());
                if (tvFullName != null) tvFullName.setText(updatedUser.getFullName());
            }
        }
    }

    private final BroadcastReceiver profileUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newUserName = intent.getStringExtra("new_name");
            String newFullName = intent.getStringExtra("new_full_name");
            if (newUserName != null && newFullName != null) {
                updateInfoAboutUser(newUserName, newFullName);
                Log.d(TAG, "Информация о пользователе обновлена на: " + newUserName + ", " + newFullName);
            }
        }
    };

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - принудительная загрузка данных");
        infoAboutUser();
        updateInfoAboutUser(sessionManager.getUsername(),
                            sessionManager.getFullName());
        setupButtons();
        registerReceiverProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - регистрация ресивера");
        infoAboutUser();
        updateInfoAboutUser(sessionManager.getUsername(),
                            sessionManager.getFullName());
        setupButtons();
        registerReceiverProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private void registerReceiverProfile() {
        try {
            IntentFilter filter = new IntentFilter("com.example.finsaver.PROFILE_UPDATED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(profileUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(profileUpdateReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка регистрации ресивера", e);
        }
    }
}
