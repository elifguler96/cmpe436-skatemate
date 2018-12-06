package com.example.elifguler.skatemate;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationAdapterViewHolder> {
    private List<Conversation> conversationData;
    private String clientUsername;
    private final ConversationAdapterOnClickHandler onClickHandler;

    public ConversationAdapter(ConversationAdapterOnClickHandler clickHandler) {
        onClickHandler = clickHandler;
    }

    public void setConversationData(List<Conversation> conversationData, String clientUsername) {
        this.conversationData = conversationData;
        this.clientUsername = clientUsername;
        notifyDataSetChanged();
    }

    @Override
    public ConversationAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.conversation, parent, false);

        return new ConversationAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapterViewHolder holder, int position) {
        Conversation conversation = conversationData.get(position);
        if (conversation != null) {
            holder.usernameTextView.setText((conversation.username1.equals(clientUsername)) ?
                    conversation.username2 : conversation.username1);

            Message message = conversation.messages.get(conversation.messages.size() - 1);
            holder.messageTextView.setText(message.messageText);

            holder.dateTextView.setText(message.date);
        }
    }

    @Override
    public int getItemCount() {
        if (conversationData == null) {
            return 0;
        }

        return conversationData.size();
    }

    public class ConversationAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView usernameTextView;
        TextView messageTextView;
        TextView dateTextView;

        public ConversationAdapterViewHolder(View view) {
            super(view);

            usernameTextView = view.findViewById(R.id.tv_username_message_list);
            messageTextView = view.findViewById(R.id.tv_message_message_list);
            dateTextView = view.findViewById(R.id.tv_date_message_list);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onClickHandler.onClick(conversationData.get(getAdapterPosition()));
        }
    }

    public interface ConversationAdapterOnClickHandler {
        void onClick(Conversation conversation);
    }
}
