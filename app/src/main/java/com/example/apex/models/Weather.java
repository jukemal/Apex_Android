package com.example.apex.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Weather implements Serializable {

    @SerializedName("weather")
    @Expose
    private List<WeatherDescription> weatherDescriptions;
    @SerializedName("main")
    @Expose
    private WeatherDetails weatherDetails;
    @SerializedName("wind")
    @Expose
    private Wind wind;
    @SerializedName("sys")
    @Expose
    private CountryDetails countryDetails;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    private boolean expanded;

    public Weather() {
    }

    public Weather(List<WeatherDescription> weatherDescriptions, WeatherDetails weatherDetails, Wind wind, CountryDetails countryDetails, Integer id, String name) {
        this.weatherDescriptions = weatherDescriptions;
        this.weatherDetails = weatherDetails;
        this.wind = wind;
        this.countryDetails = countryDetails;
        this.id = id;
        this.name = name;
    }

    public List<WeatherDescription> getWeatherDescriptions() {
        return weatherDescriptions;
    }

    public void setWeatherDescriptions(List<WeatherDescription> weatherDescriptions) {
        this.weatherDescriptions = weatherDescriptions;
    }

    public WeatherDetails getWeatherDetails() {
        return weatherDetails;
    }

    public void setWeatherDetails(WeatherDetails weatherDetails) {
        this.weatherDetails = weatherDetails;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public CountryDetails getCountryDetails() {
        return countryDetails;
    }

    public void setCountryDetails(CountryDetails countryDetails) {
        this.countryDetails = countryDetails;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public String toString() {
        return "Weather{" +
                "weatherDescriptions=" + weatherDescriptions +
                ", weatherDetails=" + weatherDetails +
                ", wind=" + wind +
                ", countryDetails=" + countryDetails +
                ", id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

