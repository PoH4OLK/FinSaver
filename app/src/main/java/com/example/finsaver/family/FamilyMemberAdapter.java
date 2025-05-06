package com.example.finsaver.family;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.R;
import com.example.finsaver.models.FamilyGroup;
import com.example.finsaver.family.MemberTransactionsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.ViewHolder> {
    private static final String TAG = "FamilyMemberAdapter";
    private List<FamilyGroup> members;
    private OnMemberRemoveListener removeListener;
    private boolean isGroupCreator;
    private int currentUserId;
    public interface OnMemberRemoveListener {
        void onRemove(int userId, int groupId);
    }

    public FamilyMemberAdapter(List<FamilyGroup> members, OnMemberRemoveListener listener, boolean isGroupCreator, int currentUserId) {
        this.members = members;
        this.removeListener = listener;
        this.isGroupCreator = isGroupCreator;
        this.currentUserId = currentUserId;
    }

    public void clearMembers() {
        this.members.clear();
        notifyDataSetChanged();
    }

    public void setGroupCreator(boolean isCreator) {
        this.isGroupCreator = isCreator;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Создание view holder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            FamilyGroup member = members.get(position);
            holder.tvName.setText(member.getName());
            holder.tvBalance.setText(String.format(Locale.getDefault(), "%.2f", member.getBalance()));
            holder.tvIncome.setText(String.format(Locale.getDefault(), "+%.2f", member.getTotalIncome()));
            holder.tvExpenses.setText(String.format(Locale.getDefault(), "-%.2f", member.getTotalExpenses()));

            // Создатель видит кнопку для всех, кроме себя
            // Обычный участник видит кнопку только для себя
            boolean showRemoveButton = (isGroupCreator && member.getUserId() != currentUserId) ||
                    (!isGroupCreator && member.getUserId() == currentUserId);

            holder.btnRemove.setVisibility(showRemoveButton ? VISIBLE : GONE);

            if (showRemoveButton) {
                holder.btnRemove.setOnClickListener(v -> {
                    if (removeListener != null) {
                        removeListener.onRemove(member.getUserId(), member.getGroupId());
                    }
                });
            } else {
                holder.btnRemove.setOnClickListener(null);
            }

            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, MemberTransactionsActivity.class);
                intent.putExtra("user_id", member.getUserId());
                context.startActivity(intent);
            });

        } catch (Exception e) {
            Log.e(TAG, "Ошибка привязки view holder", e);
        }
    }

    @Override
    public int getItemCount() {
        int count = members != null ? members.size() : 0;
        Log.d(TAG, "Количество Item: " + count);
        return count;
    }

    public void updateMembers(List<FamilyGroup> newMembers) {
        this.members.clear();
        this.members.addAll(newMembers);

        // Двойное обновление для гарантии
        new Handler(Looper.getMainLooper()).post(() -> {
            notifyDataSetChanged();
            Log.d(TAG, "Адаптер обновлен, количество элементов: " + getItemCount());
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvBalance, tvIncome, tvExpenses;
        ImageButton btnRemove;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ViewHolder создан");

            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvIncome = itemView.findViewById(R.id.tvIncome);
            tvExpenses = itemView.findViewById(R.id.tvExpenses);
            btnRemove = itemView.findViewById(R.id.btnRemove);

            if (ivAvatar == null || tvName == null || tvBalance == null ||
                    tvIncome == null || tvExpenses == null) {
                Log.e(TAG, "Некоторые представления не найдены в макете");
            }
        }
    }

    public List<FamilyGroup> getMembers() {
        return new ArrayList<>(members);
    }
}
