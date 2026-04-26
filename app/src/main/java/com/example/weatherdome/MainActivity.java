package com.example.weatherdome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import retrofit2.Call;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    /**
     * @param iconCode 和风天气返回的数字代号
     * @param imageView 你的图片控件
     */
    private void updateWeatherIcon(String iconCode, ImageView imageView) {
        if (iconCode == null) return;

        // 根据和风天气的官方代号规则进行匹配
        if (iconCode.equals("100") || iconCode.equals("150")) {
            imageView.setImageResource(R.drawable.ic_sun);
        }
        else if (iconCode.startsWith("10")) {
            imageView.setImageResource(R.drawable.ic_cloud);
        }
        else if (iconCode.startsWith("3")) {
            imageView.setImageResource(R.drawable.ic_rain);
        }
        else if (iconCode.startsWith("4")) {
            imageView.setImageResource(R.drawable.ic_snow);
        }
        else {
            imageView.setImageResource(R.drawable.ic_default_weather);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView tvWeather = findViewById(R.id.tvWeather);
        ImageView ivWeatherIcon = findViewById(R.id.ivWeatherIcon);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://n93yft9bv7.re.qweatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WeatherApiService apiService = retrofit.create(WeatherApiService.class);


        Intent intent = getIntent();
        String cityName = intent.getStringExtra("CITY_NAME");
        if(cityName!=null && !cityName.isEmpty()){
           Call<GeoResponse> geoCall = apiService.getCityId(cityName.trim(),"1554571410ae42c79632f1a5399bd192");
            geoCall.enqueue(new Callback<GeoResponse>() {
                @Override
                public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {

                    System.out.println("查ID的服务器返回码是：" + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        GeoResponse locationdata = response.body();
                        if (locationdata.location != null && !locationdata.location.isEmpty()) {
                            List<GeoResponse.Location> locationList = locationdata.location;
                            String locationId = locationList.get(0).id;
                            Call<WeatherResponse> weatherCall = apiService.getCurrentWeather(locationId, "1554571410ae42c79632f1a5399bd192");
                            weatherCall.enqueue(new Callback<WeatherResponse>() {
                                @Override
                                public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                                    System.out.println("查天气的服务器返回码是：" + response.code());
                                    if (response.isSuccessful() && response.body() != null) {
                                        WeatherResponse weatherdata = response.body();
                                        String iconCode = weatherdata.now.icon;
                                        String temp = weatherdata.now.temp;
                                        String text = weatherdata.now.text;
                                        updateWeatherIcon(iconCode,ivWeatherIcon);
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
                            tvWeather.setText("茫茫人海，找不到这座城市");
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


        }else {
            tvWeather.setText("城市名字是空的");
        }
    }
}