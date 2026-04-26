package com.example.weatherdome;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("v7/weather/now")
    Call<WeatherResponse> getCurrentWeather(
            @Query("location") String locationid,
            @Query("key") String apiKey
    );

    @GET("geo/v2/city/lookup")
    Call<GeoResponse> getCityId(
            @Query("location") String cityName,
            @Query("key") String apiKey
    );
}