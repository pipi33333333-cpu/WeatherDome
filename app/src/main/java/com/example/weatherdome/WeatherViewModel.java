package com.example.weatherdome;

import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WeatherViewModel extends ViewModel {
    private final MutableLiveData<WeatherUiState> uiState  = new MutableLiveData<>(WeatherUiState.idle());
    private final WeatherRepository weatherRepository;

    public WeatherViewModel(){
       this.weatherRepository = new WeatherRepository();
    }

    public LiveData<WeatherUiState> getUistate(){
        return uiState;
    }

    public void loadWeather(String cityName){
        if(cityName == null||cityName.trim().isEmpty()){
            weatherRepository.cancelCurrentRequests();
            uiState.setValue(WeatherUiState.error("请输入有效的城市名称"));
            return;
        }

        uiState.setValue(WeatherUiState.loading());

        String trimmedCity = cityName.trim();

        weatherRepository.getWeatherByCity(trimmedCity,new WeatherRepository.WeatherCallback(){
            @Override
            public void onSuccess(String cityName, WeatherResponse.NowData nowData) {
                int iconResId = mapIconResource(nowData.icon);
                uiState.setValue(WeatherUiState.success(
                        cityName,
                        nowData.text,
                        nowData.temp,
                        iconResId
                ));
            }
            @Override
            public void onError(String errorMessage) {
                uiState.setValue(WeatherUiState.error(errorMessage));

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
    @Override
    protected void onCleared() {
        super.onCleared();
        if(weatherRepository!=null){
            weatherRepository.cancelCurrentRequests();
        }

    }

}










