package com.example.weatherdome;

import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WeatherViewModel extends ViewModel {
    private final MutableLiveData<WeatherUiState> uiState  = new MutableLiveData<>(WeatherUiState.idle());
    private final WeatherRepository weatherRepository;
    private String currentCity;
    private boolean requestInProgress;

    public WeatherViewModel(){
        this(new WeatherRepository());
    }

    // 包内测试接缝：允许验证 ViewModel 到请求接口的完整调用链。
    WeatherViewModel(WeatherRepository weatherRepository){
        this.weatherRepository = weatherRepository;
    }

    public LiveData<WeatherUiState> getUistate(){
        return uiState;
    }

    public void loadWeather(String cityName){
        String trimmedCity = cityName == null ? "" : cityName.trim();
        if(trimmedCity.isEmpty()){
            weatherRepository.cancelCurrentRequests();
            requestInProgress = false;
            currentCity = null;
            uiState.setValue(WeatherUiState.error("请输入有效的城市名称"));
            return;
        }

        if(requestInProgress && trimmedCity.equals(currentCity)){
            return;
        }

        currentCity = trimmedCity;
        requestInProgress = true;
        uiState.setValue(WeatherUiState.loading());

        weatherRepository.getWeatherByCity(trimmedCity,new WeatherRepository.WeatherCallback(){
            @Override
            public void onSuccess(String cityName, WeatherResponse.NowData nowData) {
                requestInProgress = false;
                currentCity = null;
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
                requestInProgress = false;
                currentCity = null;
                uiState.setValue(WeatherUiState.error(errorMessage));

            }
        });
    }
    private int mapIconResource(String iconCode){
        if(iconCode==null) return R.drawable.ic_launcher_foreground;
        if(iconCode.equals("100")){
            return R.drawable.ic_snow;
        }else if(iconCode.equals("101")||iconCode.equals("104")){
            return R.drawable.ic_cloud;
        }else if(iconCode.startsWith("3")){
            return R.drawable.ic_rain;
        }else if(iconCode.startsWith("4")){
            return R.drawable.ic_snow;
        }
        return R.drawable.ic_sun;
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if(weatherRepository!=null){
            weatherRepository.cancelCurrentRequests();
        }

    }

}










