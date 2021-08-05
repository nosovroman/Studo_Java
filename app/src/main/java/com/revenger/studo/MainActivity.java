package com.revenger.studo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    final String GOOGLE_DISK = "https://drive.google.com/drive/folders/1ezjewmk_xB4NNjdYDx1ueUz_0Otzhkia?usp=sharing";

    private Toolbar mToolbar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference RootRef;

    private String currentDate, currentTime, currentUserAccess, currentUserName;
    private ImageButton SendMessageButton, SendNotificationButton, HtmlCodeButton, ShowListNames;
    private EditText MessageInputText;

    private DatabaseReference GroupNameRef, GroupMessageKeyRef, UsersRef, NotificationRef;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String tempUid, currentUserId, deviceToken, resultMessage;

    public static String[] listOfHtmlCodes = {"bold", "italic", "underline", "link", "line break", "font color"};
    private int cursorPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        RootRef = FirebaseDatabase.getInstance().getReference();

        mToolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("News");

        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child("News");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        // обновление токена при входе в приложение
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            updateDeviceToken();
        }

        InitializeControllers();

        // отображение сообщений
        GroupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // обновление времени активности пользователя при получении уведомления
                if (currentUser != null) {
                    updateUserTimeActivity();
                }

                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();

                userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
            }

            // пустые методы
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage();
            }
        });
        SendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {SendNotificationOnAllDevices();
            }
        });
        HtmlCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseTextStyle();
            }
        });

        //подлежит удалению
        ShowListNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToViewsActivity();
            }
        });
    }

    private void InitializeControllers() {
        SendMessageButton = findViewById(R.id.send_message_btn);
        SendNotificationButton = findViewById(R.id.send_notification_btn);
        HtmlCodeButton = findViewById(R.id.html_code_btn);
        MessageInputText = findViewById(R.id.input_message);
        ShowListNames = findViewById(R.id.list_names_btn);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (currentUser == null) {
            SendUserToLoginActivity();
        } else {
            // обновление времени активности пользователя
            updateUserTimeActivity();

            // доступ пользователя
            UsersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.child("access").exists()) {
                        currentUserAccess = dataSnapshot.child("access").getValue().toString();
                        if (currentUserAccess.equals("admin")) {
                            MessageInputText.setVisibility(View.VISIBLE);
                            SendMessageButton.setVisibility(View.VISIBLE);
                            SendNotificationButton.setVisibility(View.VISIBLE);
                            HtmlCodeButton.setVisibility(View.VISIBLE);
                            ShowListNames.setVisibility(View.VISIBLE);
                        } else if (currentUserAccess.equals("update")) {
                            SendUserToUpdateActivity();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        String messageKey = GroupNameRef.push().getKey();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "First write your message", Toast.LENGTH_SHORT).show();
        } else {
            // обновление времени последнего сообщения
            {
                Calendar calendar = Calendar.getInstance();

                SimpleDateFormat currentDateFormat = new SimpleDateFormat("MM.dd");
                currentDate = currentDateFormat.format(calendar.getTime());

                SimpleDateFormat currentTimeFormat = new SimpleDateFormat("HH:mm");
                currentTime = currentTimeFormat.format(calendar.getTime());

                HashMap<String, Object> stateTimeMap = new HashMap<>();
                stateTimeMap.put("time", currentTime);
                stateTimeMap.put("date", currentDate);

                RootRef.child("Numbers").child("TimeLastMessage").updateChildren(stateTimeMap);
            }

            // добавление инфы о новом сообщении
            {
                HashMap<String, Object> groupMessageKey = new HashMap<>();
                GroupNameRef.updateChildren(groupMessageKey);

                GroupMessageKeyRef = GroupNameRef.child(messageKey);

                HashMap<String, Object> messageInfoMap = new HashMap<>();
                messageInfoMap.put("name", currentUserName);
                messageInfoMap.put("message", messageText);
                messageInfoMap.put("type", "text");

                GroupMessageKeyRef.updateChildren(messageInfoMap);

                MessageInputText.setText("");
            }
        }
    }

    private void SendNotificationOnAllDevices() {
        Random random = new Random();
        final int changerForNotification = random.nextInt(1000);

        UsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    // получение id текуущего получателя уведомления
                    tempUid = item.child("uid").getValue().toString();

                    // отправка уведомления
                    NotificationRef.child(tempUid).setValue(changerForNotification);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Toast.makeText(this, "Notifications were sent", Toast.LENGTH_LONG).show();
    }

    // меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    // пункты меню
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        // логаут
        if (item.getItemId() == R.id.main_logout_option) {
            mAuth.signOut();
            SendUserToLoginActivity();
        }

        // ссылка на гугл диск
        if (item.getItemId() == R.id.main_file_option) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_DISK));
            startActivity(intent);
        }

        // переход на активность с дедлайнами
        if (item.getItemId() == R.id.main_deadline_option) {
            SendUserToDeadlineActivity();
        }
        return true;
    }

    // отправка пользователя в активность авторизации
    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    // отправка пользователя в активность дедлайнов
    private void SendUserToDeadlineActivity() {
        Intent deadlineIntent = new Intent(MainActivity.this, DeadlineActivity.class);
        startActivity(deadlineIntent);
    }

    // отправка пользователя в активность обновления приложения
    private void SendUserToUpdateActivity() {
        Intent updateIntent = new Intent(MainActivity.this, UpdateActivity.class);
        updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(updateIntent);
        finish();
    }

    // отправка пользователя в активность списка прочитавших
    private void SendUserToViewsActivity() {
        Intent viewsIntent = new Intent(MainActivity.this, ViewsActivity.class);
        startActivity(viewsIntent);
    }

    // обновление времени активности пользователя
    private void updateUserTimeActivity() {
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MM.dd");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String, Object> stateTimeMap = new HashMap<>();
        stateTimeMap.put("time", saveCurrentTime);
        stateTimeMap.put("date", saveCurrentDate);

        RootRef.child("Users").child(currentUser.getUid()).updateChildren(stateTimeMap);
    }

    // обновление токена пользователя
    private void updateDeviceToken() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                // проверка нашелся ли токен
                if (!task.isSuccessful()) {
                    return;
                }
                deviceToken = task.getResult().getToken();
                UsersRef.child(currentUserId).child("device_token").setValue(deviceToken);
            }
        });
    }

    private void chooseTextStyle() {
        cursorPosition = MessageInputText.getSelectionStart();
        String enteredText = MessageInputText.getText().toString();

        final String cursorFromStart = enteredText.subSequence(0, cursorPosition).toString();
        final String cursorToEnd = enteredText.subSequence(cursorPosition, enteredText.length()).toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true)
                .setItems(listOfHtmlCodes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (listOfHtmlCodes[i]) {
                            case "line break":
                                resultMessage = cursorFromStart + "<br>" + cursorToEnd;
                                cursorPosition+=4;
                                break;
                            case "bold":
                                resultMessage = cursorFromStart + "<b></b>" + cursorToEnd;
                                cursorPosition+=3;
                                break;
                            case "italic":
                                resultMessage = cursorFromStart + "<i></i>" + cursorToEnd;
                                cursorPosition+=3;
                                break;
                            case "underline":
                                resultMessage = cursorFromStart + "<u></u>" + cursorToEnd;
                                cursorPosition+=3;
                                break;
                            case "link":
                                resultMessage = cursorFromStart + "<a href=\"\"></a>" + cursorToEnd;
                                cursorPosition+=9;
                                break;
                            case "font color":
                                resultMessage = cursorFromStart + "<font color=\"\"></font>" + cursorToEnd;
                                cursorPosition+=13;
                                break;
                            default:
                                resultMessage = MessageInputText.getText().toString();
                                break;
                        }

                        MessageInputText.setText(resultMessage);
                        MessageInputText.setSelection(cursorPosition);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}