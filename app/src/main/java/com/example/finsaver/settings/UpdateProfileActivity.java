package com.example.finsaver.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.finsaver.R;
import com.example.finsaver.models.Users;
import com.example.finsaver.utils.SessionManager;

public class UpdateProfileActivity extends AppCompatActivity {
    private UpdateProfileViewModel viewModel;
    private SessionManager sessionManager;
    private Users user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_update);

        viewModel = new ViewModelProvider(this).get(UpdateProfileViewModel.class);
        viewModel.clearUpdateStatus();
        sessionManager = new SessionManager(this);
        user = (Users) getIntent().getSerializableExtra("user");

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etFullName = findViewById(R.id.etFullName);

        Button btnUpdateProfile = findViewById(R.id.btnUpdateProfile);

        if(user != null) {
            etUsername.setText(user.getUsername());
            etPassword.setText(user.getPassword());
            etEmail.setText(user.getEmail());
            etFullName.setText(user.getFullName());
        }

        btnUpdateProfile.setOnClickListener(v -> updateUserProfile());
    }

    private void updateUserProfile() {
        String userNameStr = ((EditText)findViewById(R.id.etUsername)).getText().toString();

        if (userNameStr.isEmpty()) {
            ((EditText)findViewById(R.id.etUsername)).setError("Введите лоигин пользователя");
            return;
        }

        String passwordStr = ((EditText)findViewById(R.id.etPassword)).getText().toString();

        if (passwordStr.isEmpty()) {
            ((EditText)findViewById(R.id.etPassword)).setError("Введите пароль");
            return;
        }

        String emailStr = ((EditText)findViewById(R.id.etEmail)).getText().toString();

        if (emailStr.isEmpty()) {
            ((EditText)findViewById(R.id.etEmail)).setError("Введите почту");
            return;
        }

        String fullNameStr = ((EditText)findViewById(R.id.etFullName)).getText().toString();

        user.setUsername(userNameStr);
        user.setPassword(passwordStr);
        user.setEmail(emailStr);
        user.setFullName(fullNameStr);

        viewModel.getUpdateUserProfileStatus().observe(this, success -> {
            if (success == null) return;
            if (success) {
                // Обновляем сессию
                sessionManager.createLoginSession(user);
                Toast.makeText(this, "Профиль обновлен", Toast.LENGTH_SHORT).show();

                // Отправляем Broadcast с новым именем
                Intent updateIntent = new Intent("com.example.finsaver.PROFILE_UPDATED");
                updateIntent.putExtra("new_name", user.getFullName());
                sendBroadcast(updateIntent);

                // Передаем обновленные данные обратно
                Intent resultIntent = new Intent();
                resultIntent.putExtra("updatedUser", user);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.changeProfile(sessionManager.getUserId(), user);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.getUpdateUserProfileStatus().removeObservers(this);
    }
}
