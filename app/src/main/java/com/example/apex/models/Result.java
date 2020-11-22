package com.example.apex.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Result implements Serializable {
    @SerializedName("formatted_address")
    @Expose
    private String formattedAddress;
    @SerializedName("place_id")
    @Expose
    private String placeId;

    public Result() {
    }

    public Result(String formattedAddress, String placeId) {
        this.formattedAddress = formattedAddress;
        this.placeId = placeId;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    @Override
    public String toString() {
        return "Result{" +
                "formattedAddress='" + formattedAddress + '\'' +
                ", placeId='" + placeId + '\'' +
                '}';
    }
}
