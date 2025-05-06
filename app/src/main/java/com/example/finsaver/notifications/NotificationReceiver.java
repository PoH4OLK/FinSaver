package com.example.finsaver.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.finsaver.MainMenuActivity;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.finsaver.NOTIFICATION_UPDATE".equals(intent.getAction())) {
            boolean hasUnread = intent.getBooleanExtra("hasUnread", false);

            // Обновляем индикатор в MainMenuActivity, если он активен
            if (context instanceof MainMenuActivity) {
                ((MainMenuActivity) context).updateNotificationIndicator(hasUnread);
            }
        }
    }
}