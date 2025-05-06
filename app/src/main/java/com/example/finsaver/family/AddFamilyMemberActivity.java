package com.example.finsaver.family;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.finsaver.R;
import com.example.finsaver.utils.SessionManager;

public class AddFamilyMemberActivity extends AppCompatActivity {
    private FamilyViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member_family);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(FamilyViewModel.class);
        viewModel.init(sessionManager.getUserId());

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etMessage = findViewById(R.id.etMessage);
        Button btnSendRequest = findViewById(R.id.btnSendRequest);

        btnSendRequest.setOnClickListener(v -> {

            String username = etUsername.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Пожалуйста введите имя пользователя", LENGTH_SHORT).show();
                return;
            }

            viewModel.sendFamilyRequest(username, message).observe(this, success -> {

                if (success == null) return;

                if (success) {
                    Toast.makeText(this, "Запрос отправлен успешно", LENGTH_SHORT).show();

                    // Отправляем сигнал о новом уведомлении
                    Intent notificationIntent = new Intent("com.example.finsaver.NEW_NOTIFICATION");
                    notificationIntent.putExtra("receiver_username", username);
                    sendBroadcast(notificationIntent);

                    finish();
                } else {
                    Toast.makeText(this, "Пользователь уже в группе или не существует", LENGTH_SHORT).show();
                }
            });
        });
    }
}
