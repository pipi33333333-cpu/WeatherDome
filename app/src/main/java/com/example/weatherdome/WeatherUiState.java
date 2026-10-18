package com.example.weatherdome;

public class WeatherUiState {
    public enum Status{
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String cityName;
    private final String weatherText;
    private final String temperature;
    private final int iconResId;
    private final String errorMessage;

    private WeatherUiState(Status status,String cityName,String weatherText,String temperature,int iconResId,String errorMessage){
        this.status=status;
        this.cityName=cityName;
        this.weatherText=weatherText;
        this.temperature=temperature;
        this.iconResId=iconResId;
        this.errorMessage=errorMessage;
    }

    public static WeatherUiState idle(){
        return new WeatherUiState(Status.IDLE,null,null,null,R.mipmap.ic_launcher,null);
    }

    public static WeatherUiState loading(){
        return new WeatherUiState(Status.LOADING,null,null,null,R.mipmap.ic_launcher,null);
    }

    public static WeatherUiState success(String cityName,String weatherText,String temperature,int iconResId){
        return new WeatherUiState(Status.SUCCESS,cityName,weatherText,temperature,iconResId,null);
    }

    public static WeatherUiState error(String errorMessage){
        return new WeatherUiState(Status.ERROR,null,null,null,R.mipmap.ic_launcher,errorMessage);
    }

    public Status getStatus(){return status;}
    public String getCityName(){return cityName;}
    public String getWeatherText(){return weatherText;}
    public String getTemperature(){return temperature;}
    public int getIconResId(){return iconResId;}
    public String getErrorMessage(){return errorMessage;}
}









