package com.example.apex.utils;

import android.content.Context;
import android.location.Location;

import java.text.DateFormat;
import java.util.Date;

public class Utils {
    public static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    public static String getLocationTitle(Context context) {
        return "Location Updates : " + DateFormat.getDateTimeInstance().format(new Date());
    }
}
