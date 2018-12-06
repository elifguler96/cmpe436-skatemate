package com.example.elifguler.skatemate;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Conversation implements Serializable {
    @SerializedName("username1")
    public String username1;

    @SerializedName("username2")
    public String username2;

    @SerializedName("messages")
    public List<Message> messages;

    public Conversation(String username1, String username2) {
        this.username1 = username1;
        this.username2 = username2;
        messages = new ArrayList<>();
    }
}
