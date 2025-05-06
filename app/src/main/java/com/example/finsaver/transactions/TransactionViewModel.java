package com.example.finsaver.transactions;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.finsaver.models.Transactions;
import com.example.finsaver.utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TransactionViewModel extends ViewModel {
    private final MutableLiveData<List<Transactions>> transactions = new MutableLiveData<>();
    private String currentFilter = "all";
    private String currentSort = "date_desc";
    private final MutableLiveData<Boolean> updateStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> transactionOperationStatus = new MutableLiveData<>();

    public LiveData<List<Transactions>> getTransactions(int userId) {
        loadTransactions(userId);
        return transactions;
    }

    public void setFilter(String filter) {
        this.currentFilter = filter.toLowerCase().replace(" ", "_");
    }

    public void setSort(String sort) {
        this.currentSort = sort.toLowerCase().replace(" ", "_");
    }

    public void loadTransactions(int userId) {
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "SELECT * FROM Transactions WHERE UserID = ?";

                // Добавляем логирование текущих фильтров
                Log.d("TransactionViewModel", "Current filter: " + currentFilter);
                Log.d("TransactionViewModel", "Current sort: " + currentSort);

                // Улучшенная фильтрация
                switch (currentFilter) {
                    case "income":
                        sql += " AND TransactionType = 'income'";
                        break;
                    case "expense":
                        sql += " AND TransactionType = 'expense'";
                        break;
                    case "last_week":
                        sql += " AND TransactionDate >= DATEADD(day, -7, GETDATE())";
                        break;
                    case "last_month":
                        sql += " AND TransactionDate >= DATEADD(month, -1, GETDATE())";
                        break;
                    case "all":
                    default:
                        break;
                }

                // Улучшенная сортировка
                switch (currentSort) {
                    case "date_asc":
                        sql += " ORDER BY TransactionDate ASC";
                        break;
                    case "date_desc":
                        sql += " ORDER BY TransactionDate DESC";
                        break;
                    case "amount_asc":
                        sql += " ORDER BY Amount ASC";
                        break;
                    case "amount_desc":
                        sql += " ORDER BY Amount DESC";
                        break;
                    default:
                        sql += " ORDER BY TransactionDate DESC";
                        break;
                }

                Log.d("TransactionViewModel", "Final SQL: " + sql);

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);

                ResultSet rs = stmt.executeQuery();
                List<Transactions> transactionList = new ArrayList<>();

                while (rs.next()) {
                    Transactions t = new Transactions();
                    t.setTransactionId(rs.getInt("TransactionID"));
                    t.setAmount(rs.getDouble("Amount"));
                    t.setDescription(rs.getString("Description"));
                    t.setType(rs.getString("TransactionType"));
                    t.setCategory(rs.getString("Category"));
                    t.setDate(new Date(rs.getTimestamp("TransactionDate").getTime()));

                    transactionList.add(t);
                }

                transactions.postValue(transactionList);
            } catch (SQLException e) {
                Log.e("TransactionViewModel", "Ошибка загрузки финансов", e);
                transactions.postValue(Collections.emptyList());
            }
        }).start();
    }

    public LiveData<Boolean> addTransaction(int userId, Transactions transaction) {
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "INSERT INTO Transactions (UserID, Amount, Description, TransactionType, Category, TransactionDate) " +
                        "VALUES (?, ?, ?, ?, ?, GETDATE())";

                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                stmt.setInt(1, userId);
                stmt.setDouble(2, transaction.getAmount());
                stmt.setString(3, transaction.getDescription());
                stmt.setString(4, transaction.getType());
                stmt.setString(5, transaction.getCategory());

                int affectedRows = stmt.executeUpdate();
                boolean success = affectedRows > 0;

                if (success) {
                    Log.d("TransactionViewModel", "Добавлено успешно");
                    loadTransactions(userId);
                    transactionOperationStatus.postValue(true); // Уведомляем об успехе
                } else {
                    Log.e("TransactionViewModel", "Не удалось добавить");
                    transactionOperationStatus.postValue(false); // Уведомляем о неудаче
                }
            } catch (SQLException e) {
                Log.e("TransactionViewModel", "Ошибка добавления", e);
                transactionOperationStatus.postValue(false); // Уведомляем об ошибке
            }
        }).start();

        return transactionOperationStatus;
    }

    public void changeTransaction(int userId, Transactions transaction){
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "UPDATE Transactions SET Amount=?, Description=?, TransactionType=?, Category=? WHERE TransactionID=? AND UserID=?";
                PreparedStatement stmt = conn.prepareStatement(sql);

                stmt.setDouble(1, transaction.getAmount());
                stmt.setString(2, transaction.getDescription());
                stmt.setString(3, transaction.getType());
                stmt.setString(4, transaction.getCategory());
                stmt.setInt(5, transaction.getTransactionId());
                stmt.setInt(6, userId);

                int affectedRows = stmt.executeUpdate();
                boolean success = affectedRows > 0;

                if (success) {
                    loadTransactions(userId);
                }
                updateStatus.postValue(success);
            } catch (SQLException e) {
                Log.e("TransactionViewModel", "Ошибка БД", e);
                updateStatus.postValue(false);
            }
        }).start();
    }

    public LiveData<Boolean> removeTransactionItem(int userId, int transactionId) {

        MutableLiveData<Boolean> result = new MutableLiveData<>();

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "DELETE FROM Transactions WHERE TransactionID = ? AND UserID = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, transactionId);
                stmt.setInt(2, userId);

                int affectedRows = stmt.executeUpdate();
                boolean success = affectedRows > 0;

                if (success) {
                    loadTransactions(userId); // Обновляем список транзакций
                }
                result.postValue(success);
            } catch (SQLException e) {
                Log.e("TransactionViewModel", "Ошибка удаления транзакции", e);
                result.postValue(false);
            }
        }).start();

        return result;
    }

    public void clearUpdateStatus() {
        updateStatus.setValue(null);
    }

    public LiveData<Boolean> getUpdateStatus() {
        return updateStatus;
    }

    public LiveData<Boolean> getTransactionOperationStatus() {
        return transactionOperationStatus;
    }

    public void clearTransactionOperationStatus() {
        transactionOperationStatus.setValue(null);
    }
}
