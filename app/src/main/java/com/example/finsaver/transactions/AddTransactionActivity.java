package com.example.finsaver.transactions;

import static android.content.ContentValues.TAG;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.finsaver.R;
import com.example.finsaver.models.Transactions;
import com.example.finsaver.utils.DatabaseHelper;
import com.example.finsaver.utils.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public class AddTransactionActivity extends AppCompatActivity {
    private TransactionViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transactions);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        sessionManager = new SessionManager(this);

        // Настройка Spinner для типа транзакции
        Spinner spinnerType = findViewById(R.id.spinnerType);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Настройка Spinner для категорий
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Кнопка сохранения
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        EditText etAmount = findViewById(R.id.etAmount);
        EditText etDescription = findViewById(R.id.etDescription);
        Spinner spinnerType = findViewById(R.id.spinnerType);
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);

        String amountStr = etAmount.getText().toString();
        String description = etDescription.getText().toString();
        String type = spinnerType.getSelectedItem().toString().equals("Поступление") ? "income" : "expense";
        String category = spinnerCategory.getSelectedItem().toString();

        if (amountStr.isEmpty()) {
            etAmount.setError("Введите сумму");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Некорректная сумма");
            return;
        }

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    Log.d("AddTransaction", "Подключение к БД: OK");
                    runOnUiThread(() -> {
                        Transactions transaction = new Transactions();
                        transaction.setAmount(amount);
                        transaction.setDescription(description);
                        transaction.setType(type); // Теперь правильно сохраняем тип
                        transaction.setCategory(category);
                        transaction.setDate(new Date());

                        int userId = sessionManager.getUserId();

                        viewModel.getTransactionOperationStatus().observe(this, success -> {
                            if (success != null) {
                                if (success) {
                                    Toast.makeText(this, "Операция выполнена успешно", Toast.LENGTH_SHORT).show();
                                    sendUpdateBroadcast(userId);
                                    finish();

                                } else {
                                    Toast.makeText(this, "Ошибка при выполнении операции", Toast.LENGTH_SHORT).show();
                                }
                                // Очищаем статус после обработки
                                viewModel.clearTransactionOperationStatus();
                            }
                        });

                        viewModel.addTransaction(userId, transaction);
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка соединения с БД", LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                Log.e("AddTransaction", "Ошибка БД", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка базы данных", LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendUpdateBroadcast(int userId) {
        Intent updateIntent = new Intent("com.example.finsaver.TRANSACTION");
        updateIntent.putExtra("user_id", userId);
        updateIntent.setPackage(getPackageName()); // Важно для Android 8+
        Log.d(TAG, "Отправка Broadcast для userID: " + userId);
        sendBroadcast(updateIntent);

        // Дублирующая отправка через 300 мс для надёжности
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sendBroadcast(updateIntent);
            Log.d(TAG, "Повторная отправка Broadcast");
        }, 300);
    }
}
