package com.example.weatherdome;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
        ProgressBar loadingSpinner = view.findViewById(R.id.loading_spinner);
        TextView tvWeather = view.findViewById(R.id.tvWeather);
        ImageView ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://n93yft9bv7.re.qweatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        String cityName = requireActivity().getIntent().getStringExtra("CITY_NAME");

        if(cityName != null && !cityName.isEmpty()){
            loadingSpinner.setVisibility(View.VISIBLE);
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

        observeViewModel();

        //savedInstanceState它是 Android 系统自带的“现场恢复包”，用来标记当前页面究竟是第一次打开，还是旋转屏幕后被系统重新创建的
        if(savedInstanceState == null&&getActivity()!=null&&getActivity().getIntent()!=null){
            String cityName = getActivity().getIntent().getStringExtra("CITY_NAME");
            if(cityName!=null&&!cityName.trim().isEmpty()){
                weatherViewModel.loadWeather(cityName);
            }

        }

    }

    private void observeViewModel(){
        weatherViewModel.getUistate().observe(getViewLifecycleOwner(),state->{
            if(state == null) return;
            switch (state.getStatus()){
                case IDLE:
                    loadingSpinner.setVisibility(View.GONE);
                    tvWeather.setText("请输入城市查询天气");
                    ivWeatherIcon.setImageResource(R.mipmap.ic_launcher);
                    break;
                case LOADING:
                    loadingSpinner.setVisibility(View.VISIBLE);
                    tvWeather.setText("正在加载中...");
                    break;
                case SUCCESS:
                    loadingSpinner.setVisibility(View.GONE);
                    tvWeather.setText("网络开小差了...");
                }
            });
        } else {
            loadingSpinner.setVisibility(View.GONE);
            tvWeather.setText("未查询到该城市。");
        }

        return view;
    }
}

