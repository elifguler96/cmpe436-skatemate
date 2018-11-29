package com.example.elifguler.skatemate;

import android.os.Parcel;
import android.os.Parcelable;

public class Message implements Parcelable {
    String username;
    String message;
    String date;

    public Message(String username, String message, String date) {
        this.username = username;
        this.message = message;
        this.date = date;
    }

    protected Message(Parcel in) {
        username = in.readString();
        message = in.readString();
        date = in.readString();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(username);
        parcel.writeString(message);
        parcel.writeString(date);
    }
}
