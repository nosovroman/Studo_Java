package com.revenger.studo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ViewsActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ArrayList<String> listOfSeen = new ArrayList<>();
    private ArrayList<String> listOfNotSeen = new ArrayList<>();
    private Date dateMsg, dateCurrentUser;
    private DatabaseReference RootRef;
    private ImageButton updateListNames;
    private TextView seenView, notSeenView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_views);

        // toolbar
        {
            mToolbar = findViewById(R.id.views_page_toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Views");
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    onBackPressed();// возврат на предыдущий activity
                }
            });
        }

        seenView = findViewById(R.id.list_seen);
        notSeenView = findViewById(R.id.list_not_seen);

        // получение даты и времени последнего сообшения
        getMsgDate();

        // распределение имен
        distributionNames();

        // кнопочка
        updateListNames = findViewById(R.id.updateListNames);
        updateListNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                distributionNames();
                Toast.makeText(ViewsActivity.this, "List was updated!", Toast.LENGTH_LONG).show();
            }
        });

    }

    // распределение имен
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void distributionNames() {
        // занесение имен в списки прочитавших/не прочитавших
        RootRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    // получение даты, времени и имени текущего получателя уведомления
                    if (item.child("date").exists() && item.child("time").exists()) {
                        String dateAndTimeUser = item.child("date").getValue().toString() + " " + item.child("time").getValue().toString();
                        String nameCurUser = item.child("name").getValue().toString();

                        // занесение в списки имен
                        try {
                            dateCurrentUser = new SimpleDateFormat("MM.dd HH:mm").parse(dateAndTimeUser);
                            // случай когда пользователь был онлайн до отправки сообщения => не читал
                            if (dateMsg != null && dateCurrentUser.compareTo(dateMsg) < 0) {
                                listOfNotSeen.add(nameCurUser);
                            } else if (dateMsg != null) {
                                listOfSeen.add(nameCurUser);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // сортировка имен по алфавиту
                Collections.sort(listOfSeen);
                Collections.sort(listOfNotSeen);

                // распределение имен
                String tempStr = String.join("\n", listOfSeen);
                seenView.setText(tempStr);

                tempStr = String.join("\n", listOfNotSeen);
                notSeenView.setText(tempStr);

                // очистка списков
                listOfSeen.clear();
                listOfNotSeen.clear();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // получение даты и времени последнего сообщения
    private void getMsgDate() {
        RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Numbers").child("TimeLastMessage").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String dateAndTime = dataSnapshot.child("date").getValue() + " " + dataSnapshot.child("time").getValue();
                try {
                    dateMsg = new SimpleDateFormat("MM.dd HH:mm").parse(dateAndTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Toast.makeText(ViewsActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
