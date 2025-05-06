package com.example.finsaver.transactions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.R;
import com.example.finsaver.models.Transactions;
import com.example.finsaver.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
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
        setContentView(R.layout.activity_transaction);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Настройка RecyclerView
        recyclerView = findViewById(R.id.recyclerViewTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Кнопка фильтрации
        Spinner spinnerFilter = findViewById(R.id.spinnerFilter);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_filters, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        // Кнопка сортировки
        Spinner spinnerSort = findViewById(R.id.spinnerSort);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        adapter.setOnItemClickListener(transaction -> {
            Intent intent = new Intent(this, UpdateTransactionActivity.class);
            intent.putExtra("transaction", transaction);
            startActivityForResult(intent, 1);
        });

        adapter.setOnRemoveListener(transactionId -> {
            new AlertDialog.Builder(this)
                    .setTitle("Удаление записи")
                    .setMessage("Вы уверены, что хотите удалить эту запись?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        int userId = sessionManager.getUserId();
                        viewModel.removeTransactionItem(userId, transactionId).observe(this, success -> {
                            if (success) {
                                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Загрузка данных
        loadTransactions();

        // Обработчики фильтров
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] filterValues = getResources().getStringArray(R.array.transaction_filters_values);
                String filter = filterValues[position];
                viewModel.setFilter(filter);
                loadTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] sortValues = getResources().getStringArray(R.array.transaction_sort_values);
                String sort = sortValues[position];
                viewModel.setSort(sort);
                loadTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTransactions() {
        int userId = sessionManager.getUserId();
        viewModel.getTransactions(userId).observe(this, transactions -> {
            if (transactions != null) {
                Log.d("TransactionsActivity", "Загружены финансы: " + transactions.size());
                for (Transactions t : transactions) {
                    Log.d("TransactionsActivity", "Финансы: " +
                            "ID=" + t.getTransactionId() +
                            ", Type=" + t.getType() +
                            ", Amount=" + t.getAmount());
                }
                adapter.setTransactions(transactions);
                updateSummary(transactions);
            } else {
                Log.e("TransactionsActivity", "Список финансов пуст");
                adapter.setTransactions(new ArrayList<>());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadTransactions();
        }
    }

    private void updateSummary(List<Transactions> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transactions t : transactions) {
            if (t.getType().equals("income")) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        TextView tvIncome = findViewById(R.id.tvTotalIncome);
        TextView tvExpense = findViewById(R.id.tvTotalExpense);
        TextView tvBalance = findViewById(R.id.tvBalance);

        tvIncome.setText(String.format(Locale.getDefault(), "+%.2f", totalIncome));
        tvExpense.setText(String.format(Locale.getDefault(), "-%.2f", totalExpense));
        tvBalance.setText(String.format(Locale.getDefault(), "%.2f", totalIncome - totalExpense));
    }
}
