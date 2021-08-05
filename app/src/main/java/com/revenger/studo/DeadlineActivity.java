package com.revenger.studo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.revenger.studo.MainActivity.listOfHtmlCodes;

public class DeadlineActivity extends AppCompatActivity {

    private FirebaseUser currentUser;
    private Toolbar mToolbar;
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapterDeadline messageAdapterDeadline;
    private FirebaseAuth mAuth;
    private DatabaseReference GroupNameRef, UsersRef, GroupMessageKeyRef;
    private RecyclerView userMessagesList;
    private LinearLayoutManager linearLayoutManager;
    private String currentUserAccess, currentUserId, resultMessage;//, currentUserName;

    private ImageButton SendMessageButton, HtmlCodeButton;
    private EditText MessageInputText;
    private int cursorPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deadline);

        mAuth = FirebaseAuth.getInstance();
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child("Deadlines");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // toolbar
        {
            mToolbar = findViewById(R.id.deadline_page_toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Deadlines");
        }

        InitializeControllers();

        // отображение сообщений
        GroupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapterDeadline.notifyDataSetChanged();

                userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
            }

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
        // отправка сообщения
        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage();
            }
        });
        HtmlCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseTextStyle();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // доступ пользователя
        UsersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.child("access").exists()) {
                    currentUserAccess = dataSnapshot.child("access").getValue().toString();
                    if (currentUserAccess.equals("admin")) {
                        MessageInputText.setVisibility(View.VISIBLE);
                        SendMessageButton.setVisibility(View.VISIBLE);
                        HtmlCodeButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        String messageKey = GroupNameRef.push().getKey();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "First write your message", Toast.LENGTH_SHORT).show();
        } else {
            // добавление инфы о новом сообщении
            {
                HashMap<String, Object> groupMessageKey = new HashMap<>();
                GroupNameRef.updateChildren(groupMessageKey);

                GroupMessageKeyRef = GroupNameRef.child(messageKey);

                HashMap<String, Object> messageInfoMap = new HashMap<>();
                //messageInfoMap.put("name", currentUserName);
                messageInfoMap.put("message", messageText);
                messageInfoMap.put("type", "text");

                GroupMessageKeyRef.updateChildren(messageInfoMap);

                MessageInputText.setText("");
            }
        }
    }

    private void InitializeControllers() {
        SendMessageButton = findViewById(R.id.send_message_btn);
        MessageInputText = findViewById(R.id.input_message);
        HtmlCodeButton = findViewById(R.id.html_code_btn);

        messageAdapterDeadline = new MessageAdapterDeadline(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapterDeadline);
    }

    private void chooseTextStyle() {
        cursorPosition = MessageInputText.getSelectionStart();
        String enteredText = MessageInputText.getText().toString();

        final String cursorFromStart = enteredText.subSequence(0, cursorPosition).toString();
        final String cursorToEnd = enteredText.subSequence(cursorPosition, enteredText.length()).toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(DeadlineActivity.this);
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
