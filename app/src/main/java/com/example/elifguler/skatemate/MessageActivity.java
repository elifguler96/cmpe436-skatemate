package com.example.elifguler.skatemate;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageActivity extends AppCompatActivity {
    EditText inputEditText;
    Button sendButton;
    RecyclerView messageRecyclerView;
    MessageAdapter messageAdapter;
    BinarySemaphore messageAdapterMutex = new BinarySemaphore(true);

    String clientUsername;
    String toUsername;
    List<Message> messages;

    ReceiveMessageThread receiveMessageThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        inputEditText = findViewById(R.id.et_message);
        sendButton = findViewById(R.id.btn_send_message);
        messageRecyclerView = findViewById(R.id.rv_message);

        clientUsername = getIntent().getStringExtra("clientUsername");
        toUsername = getIntent().getStringExtra("toUsername");

        messages = new ArrayList<>();
        Message[] messagesArray = new Gson().fromJson(getIntent().getStringExtra("messagesJson"), Message[].class);
        Collections.addAll(messages, messagesArray);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        messageRecyclerView.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter();
        messageRecyclerView.setAdapter(messageAdapter);
        messageAdapter.setMessageData(messages);
        messageRecyclerView.scrollToPosition(messages.size() - 1);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = inputEditText.getText().toString();
                SendMessageThread sendMessageThread = new SendMessageThread(message);
                sendMessageThread.start();

                inputEditText.setText("");
                inputEditText.clearFocus();
            }
        });

        receiveMessageThread = new ReceiveMessageThread();
        receiveMessageThread.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("clientUsername", clientUsername);
        startActivity(intent);
        try {
            finishAndCloseConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void finishAndCloseConnections() throws IOException {
        if (receiveMessageThread != null) {
            receiveMessageThread.closeConnection();
        }

        finish();
    }

    class SendMessageThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        String messageText;

        public SendMessageThread(String messageText) {
            this.messageText = messageText;
        }

        @Override
        public void run() {
            try {
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Message message = new Message(clientUsername, toUsername, messageText, new SimpleDateFormat().format(new Date()));

                Request request = new Request();
                request.clientUsername = clientUsername;
                request.type = RequestType.SENDMESSAGE;
                request.message = message;
                sendMessage(new Gson().toJson(request));

                messages.add(message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageAdapterMutex.P();
                        messageAdapter.setMessageData(messages);
                        messageAdapterMutex.V();

                        messageRecyclerView.scrollToPosition(messages.size() - 1);
                    }
                });

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
                    if (response.code == 9 && response.message.fromUsername.equals(toUsername)) {
                        messages.add(response.message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messageAdapterMutex.P();
                                messageAdapter.setMessageData(messages);
                                messageAdapterMutex.V();

                                messageRecyclerView.scrollToPosition(messages.size() - 1);
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }
}
