package com.example.finsaver.notifications;

import static android.view.View.*;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.R;
import com.example.finsaver.models.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private final List<Notification> notifications;
    private final NotificationClickListener listener;

    public interface NotificationClickListener {
        void onAccept(Notification notification);
        void onDecline(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, NotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvSender.setText(notification.getSenderName());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getFormattedTime());

        if (notification.getStatus() != null) {
            holder.buttonsLayout.setVisibility(GONE);
            holder.tvStatus.setVisibility(VISIBLE);

            switch (notification.getStatus()) {
                case "accepted":
                    holder.tvStatus.setText("Принято");
                    holder.tvStatus.setTextColor(Color.GREEN);
                    break;
                case "declined":
                    holder.tvStatus.setText("Отклонено");
                    holder.tvStatus.setTextColor(Color.RED);
                    break;
            }
        } else {
            holder.buttonsLayout.setVisibility(VISIBLE);
            holder.tvStatus.setVisibility(GONE);

            holder.btnAccept.setOnClickListener(v -> {
                listener.onAccept(notification);
                notification.setStatus("accepted");
                notifyItemChanged(position);
            });

            holder.btnDecline.setOnClickListener(v -> {
                listener.onDecline(notification);
                notification.setStatus("declined");
                notifyItemChanged(position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvGroup, tvMessage, tvTime, tvStatus;
        Button btnAccept, btnDecline;
        LinearLayout buttonsLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvGroup = itemView.findViewById(R.id.tvGroup);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            buttonsLayout = itemView.findViewById(R.id.buttonsLayout);
        }
    }
}