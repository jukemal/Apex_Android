// Ravindu

package com.example.apex.api.weather_api;

import com.example.apex.models.Weather;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("weather")
    Observable<Weather> getWeather(@Query("lat") double latitude, @Query("lon") double longitude);
}
