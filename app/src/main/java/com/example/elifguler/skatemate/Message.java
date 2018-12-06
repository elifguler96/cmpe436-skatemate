package com.example.elifguler.skatemate;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Message implements Serializable {
    @SerializedName("fromUsernameame")
    public String fromUsername;

    @SerializedName("toUsername")
    public String toUsername;

    @SerializedName("messageText")
    public String messageText;

    @SerializedName("date")
    public String date;

    public Message(String fromUsername, String toUsername, String messageText, String date) {
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.messageText = messageText;
        this.date = date;
    }
}
