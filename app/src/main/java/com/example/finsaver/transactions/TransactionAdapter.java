package com.example.finsaver.transactions;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.R;
import com.example.finsaver.models.Transactions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private static final String TAG = "TransactionAdapter";
    private List<Transactions> transactions;
    private OnTransactionRemoveListener removeListener;
    private boolean isReadOnlyMode;
    private OnItemClickListener onItemClickListener;

    // Конструктор для обычного режима (с возможностью удаления)
    public TransactionAdapter(List<Transactions> transactions) {
        this(transactions, false);
    }

    // Новый конструктор с возможностью указания режима "только чтение"
    public TransactionAdapter(List<Transactions> transactions, boolean isReadOnlyMode) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        this.isReadOnlyMode = isReadOnlyMode;
        Log.d(TAG, "Adapter created with " + this.transactions.size() + " transactions. ReadOnly: " + isReadOnlyMode);
    }

    public interface OnTransactionRemoveListener {
        void onRemove(int transactionId);
    }

    public interface OnItemClickListener {
        void onItemClick(Transactions transaction);
    }

    public void setTransactions(List<Transactions> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
        Log.d(TAG, "Финансы обновлены, новое количество: " + transactions.size());
    }

    public void setOnRemoveListener(OnTransactionRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Создание view holder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transactione, parent, false);
        return new ViewHolder(view, isReadOnlyMode);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Transactions transaction = transactions.get(position);
            Log.d("TransactionAdapter",
                    "Transaction #" + position +
                            ": Type=" + transaction.getType() +
                            ", Amount=" + transaction.getAmount() +
                            ", Date=" + transaction.getDate());

            // Установка значений
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
            holder.tvDescription.setText(transaction.getDescription());
            holder.tvCategory.setText(transaction.getCategory());

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(transaction.getDate()));

            if (transaction.getType().equals("income")) {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income));
                holder.ivIcon.setImageResource(R.drawable.icon_income);
            } else {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense));
                holder.ivIcon.setImageResource(R.drawable.icon_expense);
            }

            // Обработчик клика по элементу
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(transaction);
                }
            });

            // Обработчик удаления (только если не в режиме "только чтение")
            if (!isReadOnlyMode && holder.btnRemoveTransaction != null) {
                holder.btnRemoveTransaction.setOnClickListener(v -> {
                    if (removeListener != null) {
                        removeListener.onRemove(transaction.getTransactionId());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("TransactionAdapter", "Ошибка привязки финансов", e);
        }
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvAmount, tvDescription, tvCategory, tvDate;
        ImageButton btnRemoveTransaction;

        public ViewHolder(@NonNull View itemView, boolean isReadOnlyMode) {
            super(itemView);
            Log.d(TAG, "Инициализация ViewHolder. ReadOnly: " + isReadOnlyMode);

            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnRemoveTransaction = itemView.findViewById(R.id.btnRemoveTransaction);

            // Скрываем кнопку удаления в режиме "только чтение"
            if (isReadOnlyMode && btnRemoveTransaction != null) {
                btnRemoveTransaction.setVisibility(View.GONE);
            }

            // Логирование для отладки
            if (ivIcon == null) Log.e(TAG, "ivIcon не найдена в макете!");
            if (tvAmount == null) Log.e(TAG, "tvAmount не найдена в макете!");
            if (tvDescription == null) Log.e(TAG, "tvDescription не найдена в макете!");
            if (tvCategory == null) Log.e(TAG, "tvCategory не найдена в макете!");
            if (tvDate == null) Log.e(TAG, "tvDate не найдена в макете!");
            if (btnRemoveTransaction == null) Log.e(TAG, "Кнопка удаления не найдена в макете!");
        }
    }
}
