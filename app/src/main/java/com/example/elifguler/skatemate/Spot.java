package com.example.elifguler.skatemate;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Spot implements Serializable {
    @SerializedName("name")
    String name;

    @SerializedName("lat")
    double lat;

    @SerializedName("lng")
    double lng;

    public Spot(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }
}
