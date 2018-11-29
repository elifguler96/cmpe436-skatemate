package com.example.elifguler.skatemate;

import android.support.v7.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ConversationActivity extends AppCompatActivity implements ConversationAdapter.ConversationAdapterOnClickHandler {
    FloatingActionButton newConvButton;
    RecyclerView messageListRecyclerView;
    ConversationAdapter conversationAdapter;

    String clientUsername;
    ArrayList<Conversation> conversations;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        messageListRecyclerView = findViewById(R.id.rv_message_list);
        newConvButton = findViewById(R.id.btn_new_conv);

        clientUsername = getIntent().getStringExtra("clientUsername");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        messageListRecyclerView.setLayoutManager(layoutManager);

        conversationAdapter = new ConversationAdapter(this);
        messageListRecyclerView.setAdapter(conversationAdapter);

        conversations = getIntent().getParcelableArrayListExtra("conversations");
        conversationAdapter.setConversationData(conversations, clientUsername);

        ReceiveMessageThread receiveMessageThread = new ReceiveMessageThread();
        receiveMessageThread.start();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        newConvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("New Message");

                // Set up the input
                final EditText input = new EditText(context);
                input.setHint("Type clientUsername of receiver");
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String username = input.getText().toString();

                        StartMessageThread startMessageThread = new StartMessageThread(username);
                        startMessageThread.start();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onClick(Conversation conversation) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("clientUsername", clientUsername);
        intent.putExtra("toUsername", (conversation.username1.equals(clientUsername)) ?
                conversation.username2 : conversation.username1);
        intent.putStringArrayListExtra("messages", conversation.messageStrings);
        startActivity(intent);
    }

    class ReceiveMessageThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String data;
                while ((data = in.readLine()) != null && data.length() > 0) {
                    if (data.charAt(0) == '9') {
                        String username = data.substring(2, data.indexOf("$"));
                        String message = data.substring(data.indexOf("$") + 1);
                        for (Conversation c : conversations) {
                            if (c.username1.equals(username) || c.username2.equals(username)) {
                                c.messageStrings.add(message);
                                final String temp = clientUsername;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        conversationAdapter.setConversationData(conversations, temp);
                                    }
                                });
                                break;
                            }
                        }

                        String username1;
                        String username2;
                        if (clientUsername.compareTo(username) < 0) {
                            username1 = clientUsername;
                            username2 = username;
                        } else {
                            username1 = username;
                            username2 = clientUsername;
                        }

                        ArrayList<String> list = new ArrayList<>();
                        list.add(message);
                        Conversation conversation = new Conversation(username1, username2, list);
                        conversations.add(conversation);

                        final String temp = clientUsername;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                conversationAdapter.setConversationData(conversations, temp);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class StartMessageThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        String username;

        public StartMessageThread(String username) {
            this.username = username;
        }

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                sendMessage(clientUsername + "$SENDMESSAGE$" + username + "$" + "");

                String data;
                while ((data = in.readLine()) != null && data.length() > 0) {
                    if (data.charAt(0) == '8') {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "User doesn't exist", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String username = data.substring(2, data.indexOf("$"));
                        String message = data.substring(data.indexOf("$") + 1);
                        Conversation conversation = null;
                        for (Conversation c : conversations) {
                            if (c.username1.equals(username) || c.username2.equals(username)) {
                                c.messageStrings.add(message);
                                final String temp = clientUsername;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        conversationAdapter.setConversationData(conversations, temp);
                                    }
                                });
                                conversation = c;
                                break;
                            }
                        }

                        final Conversation temp = conversation;
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(context, MessageActivity.class);
                                intent.putExtra("clientUsername", clientUsername);
                                intent.putExtra("toUsername", (temp.username1.equals(clientUsername)) ?
                                        temp.username2 : temp.username1);
                                intent.putStringArrayListExtra("messages", new ArrayList<String>());
                                startActivity(intent);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendMessage(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
