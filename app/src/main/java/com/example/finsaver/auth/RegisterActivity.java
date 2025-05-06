package com.example.finsaver.auth;

import static android.widget.Toast.LENGTH_SHORT;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.finsaver.R;
import com.example.finsaver.models.Users;
import com.example.finsaver.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Инициализация полей ввода
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etFullName = findViewById(R.id.etFullName);

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            // Получаем значения из полей ввода
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();

            // Валидация введенных данных
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Необходиом заполнить все обязательные поля", LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Введите адрес эл. почты");
                return;
            }

            // Создаем объект User и заполняем его данными
            Users user = new Users();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setFullName(fullName.isEmpty() ? null : fullName); // fullName может быть null

            // Вызываем ViewModel для регистрации
            viewModel.register(user).observe(this, success -> {
                if (success) {
                    Toast.makeText(RegisterActivity.this,
                            "Регистрация прошла успешно", LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Регистрация не удалась. Пользователь, возможно, уже существует.", LENGTH_SHORT).show();
                }
            });
        });
    }
}
