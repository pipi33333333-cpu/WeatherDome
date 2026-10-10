package com.example.weatherdome;

import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;
public class WeatherViewModel extends ViewModel {
    private static final String BASE_URL = "https://n93yft9bv7.re.qweatherapi.com/";
    private static final String API_KEY = BuildConfig.QWEATHER_API_KEY;

    private final MutableLiveData<WeatherUiState> uiState  = new MutableLiveData<>(WeatherUiState.idle());
    private final WeatherApiService apiService;

    public WeatherViewModel(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WeatherApiService.class);
    }

    public LiveData<WeatherUiState> getUistate(){
        return uiState;
    }

    public void loadWeather(String cityName){
        if(cityName == null||cityName.trim().isEmpty()){
            uiState.setValue(WeatherUiState.error("请输入有效的城市名称"));
            return;
        }

        uiState.setValue(WeatherUiState.loading());

        String trimmedCity = cityName.trim();
        apiService.getCityId(trimmedCity,API_KEY).enqueue(new Callback<GeoResponse>() {
            @Override
            public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                if(response.isSuccessful()&&response.body()!=null){
                    GeoResponse locationData = response.body();
                    if(locationData.location!=null&&!locationData.location.isEmpty()){
                        String locationId = locationData.location.get(0).id;
                        fetchCurrentWeather(trimmedCity,locationId);
                    }else{
                        uiState.setValue(WeatherUiState.error("未找到对应城市信息"));
                    }
                }else{
                    uiState.setValue(WeatherUiState.error("城市查询失败:"+response.code()));
                }
            }

            @Override
            public void onFailure(Call<GeoResponse> call, Throwable t) {
                uiState.setValue(WeatherUiState.error("网络请求异常："+t.getMessage()));
            }
        });
    }
    private void fetchCurrentWeather(String cityName,String locationId){
        apiService.getCurrentWeather(locationId,API_KEY).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if(response.isSuccessful()&&response.body()!=null){
                    WeatherResponse weatherData = response.body();
                    if(weatherData.now!=null){
                        int iconResId = mapIconResource(weatherData.now.icon);
                        uiState.setValue(WeatherUiState.success(
                                cityName,
                                weatherData.now.text,
                                weatherData.now.temp,
                                iconResId
                        ));
                    }else {
                        uiState.setValue(WeatherUiState.error("天气数据解析为空"));
                    }
                }else {
                    uiState.setValue(WeatherUiState.error("获取天气失败:"+response.code()));
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                uiState.setValue(WeatherUiState.error("网络请求异常:"+t.getMessage()));
            }
        });
    }

    private int mapIconResource(String iconCode){
        if(iconCode==null) return R.mipmap.ic_launcher;
        if(iconCode.equals("100")){
            return R.drawable.ic_snow;
        }else if(iconCode.equals("101")||iconCode.equals("104")){
            return R.drawable.ic_cloud;
        }else if(iconCode.startsWith("3")){
            return R.drawable.ic_rain;
        }else if(iconCode.startsWith("4")){
            return R.drawable.ic_snow;
        }
        return R.mipmap.ic_launcher;
    }

}










