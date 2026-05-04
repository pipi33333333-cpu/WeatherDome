package com.example.weatherdome;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        TextView tvWeather = view.findViewById(R.id.tvWeather);
        ImageView ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://n93yft9bv7.re.qweatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        String cityName = requireActivity().getIntent().getStringExtra("CITY_NAME");

        if(cityName != null && !cityName.isEmpty()){
            Call<GeoResponse> geoCall = apiService.getCityId(cityName.trim(),"1554571410ae42c79632f1a5399bd192");
            geoCall.enqueue(new Callback<GeoResponse>() {
                @Override
                public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        GeoResponse locationdata = response.body();
                        if (locationdata.location != null && !locationdata.location.isEmpty()) {
                            String locationId = locationdata.location.get(0).id;
                            Call<WeatherResponse> weatherCall = apiService.getCurrentWeather(locationId, "1554571410ae42c79632f1a5399bd192");
                            weatherCall.enqueue(new Callback<WeatherResponse>() {
                                @Override
                                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        WeatherResponse weatherdata = response.body();
                                        String iconCode = weatherdata.now.icon;
                                        String temp = weatherdata.now.temp;
                                        String text = weatherdata.now.text;

                                        int localImageResId = R.mipmap.ic_launcher;
                                        if (iconCode != null) {
                                            if (iconCode.equals("100")) {
                                                localImageResId = R.drawable.ic_sun;
                                            } else if (iconCode.equals("101") || iconCode.equals("104")) {
                                                localImageResId = R.drawable.ic_cloud;
                                            } else if (iconCode.startsWith("3")) {
                                                localImageResId = R.drawable.ic_rain;
                                            } else if (iconCode.startsWith("4")) {
                                                localImageResId = R.drawable.ic_snow;
                                            }
                                        }

                                        Glide.with(requireContext())
                                                .load(localImageResId)
                                                .into(ivWeatherIcon);

                                        tvWeather.setText(cityName + "的实时天气：\n" + text + "\n温度：\n" + temp + "℃");
                                    } else {
                                        tvWeather.setText("查天气被拒，错误码：" + response.code());
                                    }
                                }

                                @Override
                                public void onFailure(Call<WeatherResponse> call, Throwable t) {
                                    tvWeather.setText("获取天气失败，请检查网络");
                                }
                            });
                        } else {
                            tvWeather.setText("找不到这座城市");
                        }
                    } else {
                        tvWeather.setText("查城市ID被拒！错误码：" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GeoResponse> call, Throwable t) {
                    tvWeather.setText("网络开小差了...");
                }
            });
        } else {
            tvWeather.setText("城市名字是空的");
        }

        return view;
    }
}

