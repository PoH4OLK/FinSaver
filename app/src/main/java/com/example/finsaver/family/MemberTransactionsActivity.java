package com.example.finsaver.family;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.R;
import com.example.finsaver.models.Transactions;
import com.example.finsaver.transactions.TransactionAdapter;
import com.example.finsaver.transactions.TransactionViewModel;
import com.example.finsaver.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberTransactionsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TransactionViewModel viewModel;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_transactions);

        // Получаем userId из Intent
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            finish();
            return;
        }

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        recyclerView = findViewById(R.id.rvTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Инициализируем адаптер с пустым списком
        adapter = new TransactionAdapter(new ArrayList<>(), true);
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
        viewModel.getTransactions(userId).observe(this, transactions -> {
            if (transactions != null) {
                Log.d("TransactionsActivity", "Загружены финансы: " + transactions.size());
                adapter.setTransactions(transactions);
                updateSummary(transactions);
            } else {
                Log.e("TransactionsActivity", "Список финансов пуст");
                adapter.setTransactions(new ArrayList<>());
            }
        });
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

        if (tvIncome != null && tvExpense != null && tvBalance != null) {
            tvIncome.setText(String.format(Locale.getDefault(), "+%.2f", totalIncome));
            tvExpense.setText(String.format(Locale.getDefault(), "-%.2f", totalExpense));
            tvBalance.setText(String.format(Locale.getDefault(), "%.2f", totalIncome - totalExpense));
        }
    }
}
