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

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ConversationActivity extends AppCompatActivity implements ConversationAdapter.ConversationAdapterOnClickHandler {
    FloatingActionButton newConvButton;
    RecyclerView messageListRecyclerView;
    ConversationAdapter conversationAdapter;
    BinarySemaphore conversationAdapterMutex = new BinarySemaphore(true);

    String clientUsername;
    List<Conversation> conversations;
    Context context = this;

    ReceiveMessageThread receiveMessageThread;

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

        receiveMessageThread = new ReceiveMessageThread();
        receiveMessageThread.start();

        GetConversationsThread getConversationsThread = new GetConversationsThread();
        getConversationsThread.start();

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
                input.setHint("Type username of receiver");
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("clientUsername", clientUsername);
        startActivity(intent);
        try {
            finishAndCloseConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onClick(Conversation conversation) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("clientUsername", clientUsername);
        intent.putExtra("toUsername", (conversation.username1.equals(clientUsername)) ?
                conversation.username2 : conversation.username1);
        intent.putExtra("messagesJson", new Gson().toJson(conversation.messages.toArray(), Message[].class));
        startActivity(intent);
        try {
            finishAndCloseConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finishAndCloseConnections() throws IOException {
        if (receiveMessageThread != null) {
            receiveMessageThread.closeConnection();
        }

        finish();
    }

    class GetConversationsThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                final Request request = new Request();
                request.clientUsername = clientUsername;
                request.type = RequestType.GETCONVERSATIONS;
                sendMessage(new Gson().toJson(request));

                String data;
                while ((data = in.readLine()) != null) {
                    Response response = new Gson().fromJson(data, Response.class);
                    if (response.code == 3) {
                        conversations = response.conversations;
                        final String temp = clientUsername;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                conversationAdapterMutex.P();
                                conversationAdapter.setConversationData(conversations, temp);
                                conversationAdapterMutex.V();
                            }
                        });
                        break;
                    }
                }

                closeConnection();
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }

    class ReceiveMessageThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Request request = new Request();
                request.clientUsername = clientUsername;
                // to send the clientUsername
                request.type = RequestType.SENDMESSAGE;
                request.message = new Message("", clientUsername, "", "");
                sendMessage(new Gson().toJson(request));

                String data;
                while ((data = in.readLine()) != null) {
                    Response response = new Gson().fromJson(data, Response.class);
                    if (response.code == 9) {
                        Message message = response.message;

                        String username1;
                        String username2;
                        if (message.fromUsername.compareTo(message.toUsername) < 0) {
                            username1 = message.fromUsername;
                            username2 = message.toUsername;
                        } else {
                            username1 = message.toUsername;
                            username2 = message.fromUsername;
                        }

                        boolean conversationExists = false;
                        for (Conversation c : conversations) {
                            if (c.username1.equals(username1) && c.username2.equals(username2)) {
                                c.messages.add(message);
                                final String temp = clientUsername;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        conversationAdapterMutex.P();
                                        conversationAdapter.setConversationData(conversations, temp);
                                        conversationAdapterMutex.V();
                                    }
                                });
                                conversationExists = true;
                            }
                        }

                        if (!conversationExists) {
                            Conversation conversation = new Conversation(username1, username2);
                            conversation.messages.add(message);
                            conversations.add(conversation);

                            final String temp = clientUsername;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    conversationAdapterMutex.P();
                                    conversationAdapter.setConversationData(conversations, temp);
                                    conversationAdapterMutex.V();
                                }
                            });
                        }
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
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
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Message message = new Message(clientUsername, username, "Hey!", new SimpleDateFormat().format(new Date()));

                Request request = new Request();
                request.clientUsername = clientUsername;
                request.type = RequestType.SENDMESSAGE;
                request.message = message;
                sendMessage(new Gson().toJson(request));

                String data;
                while ((data = in.readLine()) != null) {
                    Response response = new Gson().fromJson(data, Response.class);
                    if (response.code == 8) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "User doesn't exist", Toast.LENGTH_SHORT).show();
                            }
                        });

                        break;
                    } else if (response.code == 7){
                        String username1;
                        String username2;
                        if (message.fromUsername.compareTo(message.toUsername) < 0) {
                            username1 = message.fromUsername;
                            username2 = message.toUsername;
                        } else {
                            username1 = message.toUsername;
                            username2 = message.fromUsername;
                        }

                        Conversation conversation = null;
                        for (Conversation c : conversations) {
                            if (c.username1.equals(username1) && c.username2.equals(username2)) {
                                c.messages.add(message);
                                final String temp = clientUsername;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        conversationAdapterMutex.P();
                                        conversationAdapter.setConversationData(conversations, temp);
                                        conversationAdapterMutex.V();
                                    }
                                });
                                conversation = c;
                            }
                        }

                        if (conversation == null) {
                            conversation = new Conversation(username1, username2);
                            conversation.messages.add(message);
                            conversations.add(conversation);

                            final String temp = clientUsername;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    conversationAdapterMutex.P();
                                    conversationAdapter.setConversationData(conversations, temp);
                                    conversationAdapterMutex.V();
                                }
                            });
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
                                intent.putExtra("messagesJson", new Gson().toJson(temp.messages.toArray(), Message[].class));
                                startActivity(intent);
                            }
                        });

                        break;
                    }
                }

                closeConnection();

                finishAndCloseConnections();
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }
}
