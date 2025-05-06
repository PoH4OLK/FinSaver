package com.example.finsaver;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finsaver.auth.LoginActivity;
import com.example.finsaver.family.AddFamilyMemberActivity;
import com.example.finsaver.family.FamilyViewModel;
import com.example.finsaver.family.FamilyMemberAdapter;
import com.example.finsaver.models.Notification;
import com.example.finsaver.notifications.NotificationAdapter;
import com.example.finsaver.notifications.NotificationReceiver;
import com.example.finsaver.settings.ProfileSettingsActivity;
import com.example.finsaver.transactions.AddTransactionActivity;
import com.example.finsaver.transactions.TransactionsActivity;
import com.example.finsaver.utils.DatabaseHelper;
import com.example.finsaver.utils.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainMenuActivity extends AppCompatActivity {
    private static final String TAG = "MainMenuActivity";
    private FamilyViewModel familyViewModel;
    private RecyclerView rvFamilyMembers;
    private SessionManager sessionManager;
    private FamilyMemberAdapter adapter;
    private ImageView notificationIndicator;
    private NotificationReceiver notificationReceiver;
    private AlertDialog notificationsDialog;
    private Button btnCreateGroup;
    private Button btnDisbandGroup;
    private TextView tvWelcome;
    private boolean isGroupCreator = false;
    private boolean isFamilyExpanded = true;
    private LinearLayout llFamilyContent;
    private ImageView ivExpandCollapse;
    private final BroadcastReceiver transactionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.hasExtra("user_id")) return;

            int userId = intent.getIntExtra("user_id", -1);
            if (userId != sessionManager.getUserId()) return;

            if (!isFinishing() && !isDestroyed()) {
                familyViewModel.loadFamilyMembers();
                setupWelcomeMessage();

                // Обновление через ViewModel вместо задержки
                familyViewModel.setRequireUiUpdate(true);
            }
        }
    };

    private final BroadcastReceiver profileUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // 1. Проверка Intent и действия
                if (intent == null || !intent.hasExtra("new_name")) {
                    return;
                }

                // 2. Проверка актуальности контекста
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // 3. Безопасное извлечение данных
                String newName = intent.getStringExtra("new_name");
                if (newName == null || newName.isEmpty()) {
                    Log.w(TAG, "Получено пустое имя");
                    return;
                }

                // 4. Обновление UI на главном потоке
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        updateWelcomeMessage(newName);
                        Log.i(TAG, "Приветствие обновлено для: " + newName);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка обработки обновления профиля", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (new SessionManager(this).isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Инициализация SessionManager (проверка авторизации)
        sessionManager = new SessionManager(this);
        Log.d(TAG, "Сессия инициализирована. Пользователь вошел в систему: " + sessionManager.isLoggedIn());

        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Пользователь не вошел в систему, перенаправляется на вход в систему");
            redirectToLogin();
            return;
        }

        // Инициализация ViewModel (первым делом)
        familyViewModel = new ViewModelProvider(this).get(FamilyViewModel.class);
        familyViewModel.init(sessionManager.getUserId());

        // Настройка UI компонентов
        notificationIndicator = findViewById(R.id.ivNotificationIndicator);
        setupRecyclerView();
        setupWelcomeMessage();
        setupButtons();
        setupAdditionalButtons();

        // Установка наблюдателей (после инициализации ViewModel)
        familyViewModel.getIsGroupCreator().observe(this, isCreator -> {
                    if (adapter != null) {
                        adapter.setGroupCreator(isCreator != null && isCreator);
                    }
                    updateUI();
                });

        familyViewModel.getFamilyMembers().observe(this, members -> {
            try {
                if (members == null || members.isEmpty()) {
                    showEmptyState();
                    // Проверяем, был ли пользователь удален из группы
                    if (!familyViewModel.getIsGroupCreator().getValue()) {
                        updateUI();
                    }
                } else {
                    adapter.updateMembers(members);
                    showMembersList();
                }
                updateUI();
            } catch (Exception e) {
                showEmptyState();
            }
        });

        familyViewModel.getInvitationAccepted().observe(this, accepted -> {
            if (accepted != null && accepted) {
                familyViewModel.loadFamilyMembers();
                if (notificationsDialog != null && notificationsDialog.isShowing()) {
                    notificationsDialog.dismiss();
                }
                checkForInvitations();
            }
        });
        tvWelcome = findViewById(R.id.tvWelcome);
        setupCollapseExpand();
    }


    private void setupAdditionalButtons() {
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnDisbandGroup = findViewById(R.id.btnDisbandGroup);

        btnCreateGroup.setOnClickListener(v -> {
            familyViewModel.createFamilyGroup().observe(this, success -> {
                if (success) {
                    Toast.makeText(this, "Группа создана", Toast.LENGTH_SHORT).show();
                    updateUI(); // Явное обновление UI
                } else {
                    Toast.makeText(this, "Ошибка создания группы", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnDisbandGroup.setOnClickListener(v -> {
            if (adapter.getItemCount() == 0) return;

            int groupId = adapter.getMembers().get(0).getGroupId();

            new AlertDialog.Builder(this)
                    .setTitle("Подтверждение")
                    .setMessage("Вы точно хотите расформировать группу? Все участники будут удалены.")
                    .setPositiveButton("Да", (dialog, which) -> {
                        // Показываем прогресс
                        ProgressDialog progress = new ProgressDialog(this);
                        progress.setMessage("Расформирование группы...");
                        progress.setCancelable(false);
                        progress.show();

                        familyViewModel.disbandGroup(groupId).observe(this, success -> {
                            progress.dismiss();

                            if (success) {
                                // Явное обновление всех компонентов
                                adapter.updateMembers(Collections.emptyList());
                                updateUI();

                                // Дополнительная проверка состояния
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    familyViewModel.loadFamilyMembers();
                                }, 300);

                                Toast.makeText(this, "Группа успешно расформирована", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Ошибка при расформировании группы", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void setupCollapseExpand() {
        llFamilyContent = findViewById(R.id.llFamilyContent);
        ivExpandCollapse = findViewById(R.id.ivExpandCollapse);
        TextView tvFamilyTitle = findViewById(R.id.tvFamilyMembersTitle);

        // Обработчик клика по заголовку
        tvFamilyTitle.setOnClickListener(v -> toggleFamilyExpand());

        // Обработчик клика по иконке
        ivExpandCollapse.setOnClickListener(v -> toggleFamilyExpand());
    }


    private void toggleFamilyExpand() {
        isFamilyExpanded = !isFamilyExpanded;

        // Анимация перед изменением
        TransitionManager.beginDelayedTransition(
                (ViewGroup) llFamilyContent.getParent(),
                new AutoTransition()
                        .setDuration(200)
                        .setInterpolator(new FastOutSlowInInterpolator())
        );

        if (isFamilyExpanded) {
            llFamilyContent.setVisibility(View.VISIBLE);

            ivExpandCollapse.setImageResource(R.drawable.icon_expand_less);
            if(adapter.getItemCount() == 0) {
                familyViewModel.loadFamilyMembers();
            }
        } else {
            llFamilyContent.setVisibility(View.GONE);
            ivExpandCollapse.setImageResource(R.drawable.icon_expand_more);
        }
    }

    private void updateUI() {
        runOnUiThread(() -> {
            try {
                boolean hasMembers = adapter != null && adapter.getItemCount() > 0;
                boolean isCreator = familyViewModel.getIsGroupCreator().getValue() != null &&
                        familyViewModel.getIsGroupCreator().getValue();

                // Если пользователь удалил себя - показываем пустое состояние
                if (!hasMembers && !isCreator) {
                    showEmptyState();
                    btnCreateGroup.setVisibility(View.VISIBLE);
                    btnDisbandGroup.setVisibility(View.GONE);
                    findViewById(R.id.btnAddFamilyMember).setVisibility(View.GONE);
                    return;
                }

                btnCreateGroup.setVisibility(!hasMembers ? View.VISIBLE : View.GONE);
                btnDisbandGroup.setVisibility(isCreator && hasMembers ? View.VISIBLE : View.GONE);
                findViewById(R.id.btnAddFamilyMember).setVisibility(isCreator && hasMembers ? View.VISIBLE : View.GONE);

                rvFamilyMembers.setVisibility(hasMembers ? View.VISIBLE : View.GONE);
                findViewById(R.id.tvEmptyFamily).setVisibility(hasMembers ? View.GONE : View.VISIBLE);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении UI", e);
            }
        });
    }

    private void setupRecyclerView() {
        rvFamilyMembers = findViewById(R.id.rvFamilyMembers);
        adapter = new FamilyMemberAdapter(new ArrayList<>(), (userId, groupId) -> {
            familyViewModel.removeFamilyMember(userId, groupId).observe(this, success -> {
                if (success) {
                    Toast.makeText(this,
                            userId == sessionManager.getUserId()
                                    ? "Вы вышли из группы"
                                    : "Участник удален",
                            Toast.LENGTH_SHORT).show();

                    if (userId == sessionManager.getUserId()) {
                        adapter.clearMembers(); // Немедленно очищаем список
                        updateUI(); // Принудительно обновляем интерфейс

                        // Дополнительная проверка через 500 мс
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            familyViewModel.loadFamilyMembers();
                        }, 500);
                    }
                } else {
                    Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            });
        }, isGroupCreator, sessionManager.getUserId());
        rvFamilyMembers.setLayoutManager(new LinearLayoutManager(this));
        rvFamilyMembers.setAdapter(adapter);
    }

    private void setupWelcomeMessage() {
        if (tvWelcome != null) {
            String username = sessionManager.getFullName();
            Log.d(TAG, "Настройка приветственного сообщения для пользователя: " + username);
            tvWelcome.setText("Добро пожаловать, " + username + "!");
        } else {
            Log.e(TAG, "Текстовое поле приветствия не найдено");
        }
    }

    private void updateWelcomeMessage(String name) {
        if (tvWelcome != null) {
            tvWelcome.setText("Добро пожаловать, " + name + "!");
        }
    }

    private void setupButtons() {
        try {

            View btnAddTransaction = findViewById(R.id.btnAddTransaction);
            if (btnAddTransaction != null) {
                btnAddTransaction.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка Добавить финансы");
                    startActivity(new Intent(MainMenuActivity.this, AddTransactionActivity.class));
                });
            }

            View btnProfileSettings = findViewById(R.id.btnProfileSettings);
            if (btnProfileSettings != null) {
                btnProfileSettings.setOnClickListener(v -> {
                    startActivity(new Intent(this, ProfileSettingsActivity.class));
                });
            }

            View btnViewTransactions = findViewById(R.id.btnViewTransactions);
            if (btnViewTransactions != null) {
                btnViewTransactions.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка просмотра финансов");
                    startActivity(new Intent(this, TransactionsActivity.class));
                });
            }

            View btnAddFamilyMember = findViewById(R.id.btnAddFamilyMember);
            if (btnAddFamilyMember != null) {
                btnAddFamilyMember.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка Добавить в группу");
                    startActivity(new Intent(this, AddFamilyMemberActivity.class));
                });
            }

            Button btnFamilyGroup = findViewById(R.id.btnFamilyGroup);
            if (btnFamilyGroup != null) {
                btnFamilyGroup.setOnClickListener(v -> {
                    Log.d(TAG, "Нажата кнопка Приглашения");
                    showNotificationDialog();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке кнопок", e);
        }
    }

    private void showNotificationDialog() {
        new Thread(() -> {
            try {
                List<Notification> notifications = new DatabaseHelper()
                        .getNotifications(sessionManager.getUserId());

                runOnUiThread(() -> {
                    if (notifications.isEmpty()) {
                        Toast.makeText(this, "Нет новых приглашений", LENGTH_SHORT).show();
                    } else {
                        showNotificationsList(notifications);
                    }
                });
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка загрузки уведомлеий", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка загрузки приглашений", LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showNotificationsList(List<Notification> notifications) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notifications, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        NotificationAdapter adapter = new NotificationAdapter(notifications, new NotificationAdapter.NotificationClickListener() {
            @Override
            public void onAccept(Notification notification) {
                familyViewModel.acceptInvitation(
                        Integer.parseInt(notification.getGroupId()),
                        sessionManager.getUserId()
                );
                updateNotificationIndicator(false);
            }

            @Override
            public void onDecline(Notification notification) {
                familyViewModel.declineInvitation(
                        Integer.parseInt(notification.getGroupId()),
                        sessionManager.getUserId()
                );
                updateNotificationIndicator(false);
            }
        });

        recyclerView.setAdapter(adapter);

        notificationsDialog = new AlertDialog.Builder(this)
                .setTitle("Приглашения в группы")
                .setView(dialogView)
                .setPositiveButton("Закрыть", (dialog, which) -> {
                    // При закрытии диалога проверяем уведомления
                    checkForInvitations();
                })
                .show();
    }


    private void checkForInvitations() {
        new Thread(() -> {
            try {
                List<Notification> notifications = new DatabaseHelper()
                        .getNotifications(sessionManager.getUserId());

                boolean hasUnread = notifications.stream()
                        .anyMatch(n -> !n.isRead() && n.getStatus() == null);

                runOnUiThread(() -> {
                    updateNotificationIndicator(hasUnread);
                    // Обновляем список, если диалог открыт
                    if (notificationsDialog != null && notificationsDialog.isShowing()) {
                        showNotificationsList(notifications);
                    }
                });
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка проверки уведомлений", e);
            }
        }).start();
    }

    private void showEmptyState() {
        runOnUiThread(() -> {
            try {
                View emptyView = findViewById(R.id.tvEmptyFamily);
                if (emptyView != null) emptyView.setVisibility(VISIBLE);
                if (rvFamilyMembers != null) rvFamilyMembers.setVisibility(GONE);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка, показывающая пустое состояние", e);
            }
        });
    }

    private void showMembersList() {
        runOnUiThread(() -> {
            try {
                View emptyView = findViewById(R.id.tvEmptyFamily);
                if (emptyView != null) emptyView.setVisibility(GONE);
                if (rvFamilyMembers != null) rvFamilyMembers.setVisibility(VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка вывода списка участников", e);
            }
        });
    }

    private void redirectToLogin() {
        Log.d(TAG, "Направление на авторизацию");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            unregisterReceiver(notificationReceiver);
            notificationReceiver = null;
        }
    }

    public void updateNotificationIndicator(boolean hasUnread) {
        runOnUiThread(() -> {
            ImageView indicator = findViewById(R.id.ivNotificationIndicator);
            if (indicator != null) {
                if (hasUnread) {
                    // Показываем с анимацией
                    indicator.setVisibility(VISIBLE);
                    Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
                    indicator.startAnimation(pulse);
                } else {
                    // Скрываем и останавливаем анимацию
                    indicator.clearAnimation();
                    indicator.setVisibility(GONE);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - принудительная загрузка данных");
        familyViewModel.loadFamilyMembers();
        checkForInvitations();
        updateUI();
        updateWelcomeMessage(sessionManager.getFullName());
        registerReceiverTransaction();
        registerReceiverProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - регистрация ресивера");
        registerReceiverTransaction();
        registerReceiverProfile();
        updateWelcomeMessage(sessionManager.getFullName());
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(transactionUpdateReceiver);
        unregisterReceiver(profileUpdateReceiver);
    }

    private void registerReceiverTransaction() {
        try {
            IntentFilter filter = new IntentFilter("com.example.finsaver.TRANSACTION");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(transactionUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(transactionUpdateReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка регистрации ресивера", e);
        }
    }

    private void registerReceiverProfile() {
        try {
            IntentFilter filter = new IntentFilter("com.example.finsaver.PROFILE_UPDATED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(profileUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(profileUpdateReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка регистрации ресивера", e);
        }
    }
}