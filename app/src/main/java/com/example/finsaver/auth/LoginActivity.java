package com.example.finsaver.auth;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.finsaver.MainMenuActivity;
import com.example.finsaver.R;
import com.example.finsaver.models.Users;
import com.example.finsaver.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private LoginViewModel viewModel;
    private CheckBox chRememberMe;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        chRememberMe = findViewById(R.id.chRememberMe);

        if (sessionManager.isRememberMe() && sessionManager.isLoggedIn()) {
            proceedToMainMenu();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            viewModel.login(username, password).observe(this, success -> {
                if (success == null) return;

                if (success) {
                    Users user = viewModel.getCurrentUser(); // Получаем полный объект
                    sessionManager.setRememberMe(
                            chRememberMe.isChecked(),
                            viewModel.getCurrentUser()
                    );

                    sessionManager.createLoginSession(user); // Сохраняем всю сессию
                    proceedToMainMenu();
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка авторизации", LENGTH_SHORT).show();
                }
            });
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void proceedToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}