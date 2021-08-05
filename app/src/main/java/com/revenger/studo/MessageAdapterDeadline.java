package com.revenger.studo;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MessageAdapterDeadline extends RecyclerView.Adapter<MessageAdapterDeadline.MessageViewHolder> {
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private ProgressDialog loadingBar;

    public MessageAdapterDeadline(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView subjectDeadlineText;

        public MessageViewHolder (@NonNull View itemView) {
            super(itemView);
            subjectDeadlineText = itemView.findViewById(R.id.subject_deadline_text);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.custom_messages_layout_dead, viewGroup, false);
        mAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, final int position) {

        Messages messages = userMessagesList.get(position);

        String messageSenderId = mAuth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(messageSenderId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        messageViewHolder.subjectDeadlineText.setVisibility(View.VISIBLE);
        messageViewHolder.subjectDeadlineText.setBackgroundResource(R.drawable.receiver_messages_layout);
        messageViewHolder.subjectDeadlineText.setTextColor(Color.BLACK);
        messageViewHolder.subjectDeadlineText.setText(Html.fromHtml(messages.getMessage()));
        messageViewHolder.subjectDeadlineText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }
}
