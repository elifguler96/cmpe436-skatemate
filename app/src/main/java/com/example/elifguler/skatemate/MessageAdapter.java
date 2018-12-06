package com.example.elifguler.skatemate;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageAdapterViewHolder> {
    private List<Message> messageData;

    public void setMessageData(List<Message> messageData) {
        this.messageData = messageData;
        notifyDataSetChanged();
    }

    @Override
    public MessageAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.message, parent, false);

        return new MessageAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapterViewHolder holder, int position) {
        Message message = messageData.get(position);
        if (message != null) {
            holder.usernameTextView.setText(message.fromUsername);
            holder.messageTextView.setText(message.messageText);
            holder.dateTextView.setText(message.date);
        }
    }

    @Override
    public int getItemCount() {
        if (messageData == null) {
            return 0;
        }

        return messageData.size();
    }

    public class MessageAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        TextView messageTextView;
        TextView dateTextView;

        public MessageAdapterViewHolder(View view) {
            super(view);

            usernameTextView = view.findViewById(R.id.tv_username);
            messageTextView = view.findViewById(R.id.tv_message);
            dateTextView = view.findViewById(R.id.tv_date);
        }
    }
}
