package com.example.apex.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Accident {
    private @DocumentId
    String id;
    private double latitude;
    private double longitude;
    private int count;
    private @ServerTimestamp
    Date timestamp;

    public Accident() {
    }

    public Accident(double latitude, double longitude, int count) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
    }

    public Accident(String id, double latitude, double longitude, int count, Date timestamp) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.count = count;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Accident{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", count=" + count +
                ", timestamp=" + timestamp +
                '}';
    }
}
