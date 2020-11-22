package com.example.apex.models;

import com.google.android.gms.maps.model.LatLng;

public class WeatherWrapper {
    private LatLng latLng;
    private Weather weather;
    private GeoCode geoCode;

    public WeatherWrapper() {
    }

    public WeatherWrapper(LatLng latLng, Weather weather, GeoCode geoCode) {
        this.latLng = latLng;
        this.weather = weather;
        this.geoCode = geoCode;
    }

    public WeatherWrapper(LatLng latLng) {
        this.latLng = latLng;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public GeoCode getGeoCode() {
        return geoCode;
    }

    public void setGeoCode(GeoCode geoCode) {
        this.geoCode = geoCode;
    }

    @Override
    public String toString() {
        return "WeatherWrapper{" +
                "latLng=" + latLng +
                ", weather=" + weather +
                ", geoCode=" + geoCode +
                '}';
    }
}
