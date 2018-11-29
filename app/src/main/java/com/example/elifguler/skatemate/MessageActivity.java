package com.example.elifguler.skatemate;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        inputEditText = findViewById(R.id.et_message);
        sendButton = findViewById(R.id.btn_send_message);
        messageRecyclerView = findViewById(R.id.rv_message);

        clientUsername = getIntent().getStringExtra("clientUsername");
        toUsername = getIntent().getStringExtra("toUsername");
        ArrayList<String> messageStrings = getIntent().getStringArrayListExtra("messages");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        messageRecyclerView.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter();
        messageRecyclerView.setAdapter(messageAdapter);

        messages = new ArrayList<>();
        for (String messageString : messageStrings) {
            String username = messageString.substring(0, messageString.indexOf("$"));
            String messageStr = messageString.substring(messageString.indexOf("$") + 1, messageString.lastIndexOf("$"));
            String date = messageString.substring(messageString.lastIndexOf("$"));
            Message message = new Message(username, messageStr, date);
            messages.add(message);
        }

        messageAdapter.setMessageData(messages);

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

        ReceiveMessageThread receiveMessageThread = new ReceiveMessageThread();
        receiveMessageThread.start();
    }

    class SendMessageThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        String message;

        public SendMessageThread(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                sendMessage(clientUsername + "$SENDMESSAGE$" + toUsername + "$" + message);

                messages.add(new Message(toUsername, message, new SimpleDateFormat().format(new Date())));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageAdapterMutex.P();
                        messageAdapter.setMessageData(messages);
                        messageAdapterMutex.V();
                    }
                });
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

                        Log.e("wow", "flr");

                        messages.add(new Message(username, message, new SimpleDateFormat().format(new Date())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messageAdapterMutex.P();
                                messageAdapter.setMessageData(messages);
                                messageAdapterMutex.V();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class BinarySemaphore { // used for mutual exclusion
    private boolean value;

    BinarySemaphore(boolean initValue) {
        value = initValue;
    }

    public synchronized void P() { // atomic operation // blocking
        while (!value) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        value = false;
    }

    public synchronized void V() { // atomic operation // non-blocking
        value = true;
        notify(); // wake up a process from the queue
    }
}
