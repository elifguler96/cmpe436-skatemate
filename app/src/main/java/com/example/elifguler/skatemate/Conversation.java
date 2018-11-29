package com.example.elifguler.skatemate;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Conversation implements Parcelable {
    String username1;
    String username2;
    ArrayList<String> messageStrings;

    public Conversation(String username1, String username2, ArrayList<String> messageStrings) {
        this.username1 = username1;
        this.username2 = username2;
        this.messageStrings = messageStrings;
    }

    protected Conversation(Parcel in) {
        username1 = in.readString();
        username2 = in.readString();
        messageStrings = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username1);
        dest.writeString(username2);
        dest.writeStringList(messageStrings);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
        @Override
        public Conversation createFromParcel(Parcel in) {
            return new Conversation(in);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };
}
