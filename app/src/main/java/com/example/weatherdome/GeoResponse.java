package com.example.weatherdome;
import java.util.List;
public class GeoResponse {
    public String code;
    public List<Location> location;

    public static class Location{
        public String name;
        public String id;
    }
}
