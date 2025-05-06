package com.example.finsaver.transactions;

import android.os.Bundle;
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
import com.example.finsaver.utils.SessionManager;

public class UpdateTransactionActivity extends AppCompatActivity {

    private TransactionViewModel viewModel;
    private SessionManager sessionManager;
    private Transactions transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_transactions);

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.clearUpdateStatus();
        sessionManager = new SessionManager(this);
        transaction = (Transactions) getIntent().getSerializableExtra("transaction");

        // Инициализация UI
        EditText etAmount = findViewById(R.id.etAmount);
        EditText etDescription = findViewById(R.id.etDescription);
        Spinner spinnerType = findViewById(R.id.spinnerType);
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        Button btnUpdate = findViewById(R.id.btnUpdate);

        // Заполнение данных
        if (transaction != null) {
            etAmount.setText(String.valueOf(transaction.getAmount()));
            etDescription.setText(transaction.getDescription());

            // Настройка Spinner
            ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.transaction_types, android.R.layout.simple_spinner_item);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
            spinnerType.setSelection(transaction.getType().equals("income") ? 0 : 1);

            // Настройка Spinner для категорий
            ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                    R.array.transaction_categories, android.R.layout.simple_spinner_item);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(categoryAdapter);

            int categoryPosition = getCategoryPosition(transaction.getCategory());
            if (categoryPosition != -1) {
                spinnerCategory.setSelection(categoryPosition);
            }
        }

        btnUpdate.setOnClickListener(v -> changeTransaction());
    }

    private int getCategoryPosition(String category) {
        String[] categories = getResources().getStringArray(R.array.transaction_categories);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    private void changeTransaction() {
        String amountStr = ((EditText)findViewById(R.id.etAmount)).getText().toString();

        if (amountStr.isEmpty()) {
            ((EditText)findViewById(R.id.etAmount)).setError("Введите сумму");
            return;
        }

        String descriptionStr  = ((EditText)findViewById(R.id.etDescription)).getText().toString();

        String spinnerTypeStr = (((Spinner)findViewById(R.id.spinnerType)).getSelectedItem().toString().equals("Поступление") ? "income" : "expense");

        String spinnerCategoryStr = ((Spinner)findViewById(R.id.spinnerCategory)).getSelectedItem().toString();

        try {
            // Обновляем данные транзакции
            transaction.setAmount(Double.parseDouble(amountStr));
            transaction.setDescription(descriptionStr);
            transaction.setType(spinnerTypeStr);
            transaction.setCategory(spinnerCategoryStr);

            // Наблюдаем за статусом обновления
            viewModel.getUpdateStatus().observe(this, success -> {
                if (success == null) return; // Игнорируем null

                if (success) {
                    Toast.makeText(this, "Транзакция обновлена", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                }
            });

            // Запускаем обновление
            viewModel.changeTransaction(sessionManager.getUserId(), transaction);

        } catch (NumberFormatException e) {
            ((EditText)findViewById(R.id.etAmount)).setError("Некорректная сумма");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.getUpdateStatus().removeObservers(this);
    }
}
