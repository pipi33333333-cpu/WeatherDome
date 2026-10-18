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

import androidx.lifecycle.ViewModelProvider;

public class WeatherFragment extends Fragment {
    private ProgressBar loadingSpinner;
    private TextView tvWeather;
    private ImageView ivWeatherIcon;
    private WeatherViewModel weatherViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

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
                    String displayText = state.getCityName()+"\n"+
                            state.getWeatherText()+"\n\n"+
                            state.getTemperature()+"°C";
                    tvWeather.setText(displayText);
                    Glide.with(this)
                            .load(state.getIconResId())
                            .into(ivWeatherIcon);
                    break;
                case  ERROR:
                    loadingSpinner.setVisibility(View.GONE);
                    tvWeather.setText(state.getErrorMessage());
                    ivWeatherIcon.setImageResource(R.mipmap.ic_launcher);
                    break;
            }

        });
    }
}







