package com.example.weatherdome;

public class WeatherResponse {
    public String code;
    public String updateTime;
    public static class NowData {
        public String temp;
        public String text;
        public String windDir;
        public String icon;
    }

    public NowData now;

}
