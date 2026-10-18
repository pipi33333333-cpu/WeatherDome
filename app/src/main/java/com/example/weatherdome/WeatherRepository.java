package com.example.weatherdome;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//该类负责初始化Retrofit并链式调度“城市查询”和“天气获取”，对外暴露统一的回调接口WeatherCallback：

public class WeatherRepository {
    private static final String BASE_URL = "https://n93yft9bv7.re.qweatherapi.com/";
    private static final String API_KEY = BuildConfig.QWEATHER_API_KEY;
    private final WeatherApiService apiService;
    /// 保存两级请求的call引用，便于主动cancel()
    private Call<GeoResponse> cityCall;
    private Call<WeatherResponse> weatherCall;
    //递增请求id，用于识别旧回调
    private int currentRequestId = 0;
    /**
     * 业务数据回调接口，避免向 ViewModel 泄露底层 Retrofit 的 Call/Response
     */
    public interface WeatherCallback {
        void onSuccess(String cityName,WeatherResponse.NowData nowData);
        void onError(String errorMessage);
    }
    public WeatherRepository(){
        //这里使用了设计模式中的建造者模式，通过BUilder可以链式调用传入各种参数
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(WeatherApiService.class);
    }

    // 包内测试接缝：测试可注入手动调度回调的服务，不创建真实网络请求。
    WeatherRepository(WeatherApiService apiService){
        this.apiService = apiService;
    }

    /**
     * 对外查询入口：先查城市 ID，再查实时天气
     */
    public void getWeatherByCity(String cityName,WeatherCallback callback){
        cancelCurrentRequests();
        //++前置先自增再赋值，++后置先赋值再自增
        final int requestId = ++currentRequestId;

        cityCall = apiService.getCityId(cityName,API_KEY);

        cityCall.enqueue(new Callback<GeoResponse>() {
            @Override
            public void onResponse(Call<GeoResponse> call, Response<GeoResponse> response) {
                if(requestId!=currentRequestId||call.isCanceled()){
                    return;
                }
                if(response.isSuccessful()&&response.body()!=null){
                    GeoResponse locationData = response.body();
                    if(locationData.location!=null&&!locationData.location.isEmpty()){
                        String locationId = locationData.location.get(0).id;
                        fetchCurrentWeather(requestId,cityName,locationId,callback);
                    }else {
                        callback.onError("未找到对应城市信息");
                    }
                }else {
                    //已在onResponse里面说明链路畅通是HTTP错误
                    callback.onError("城市查询失败,HTTP错误码:"+response.code());

                    }
                }
            @Override
            public void onFailure(Call<GeoResponse> call, Throwable t) {
                //已过期的查询或者主动取消产生的失败，都一样忽略
                if(requestId!=currentRequestId||call.isCanceled()){
                    return;
                }
                callback.onError("网络请求异常:"+t.getMessage());
            }
        });
    }
    private void fetchCurrentWeather(int requestId,String cityName,String locationId,WeatherCallback callback){
        //第二次校验，防第一级查完才被覆盖的请求发起第二级请求
        if(requestId!=currentRequestId){
            return;
        }
        weatherCall = apiService.getCurrentWeather(locationId,API_KEY);

        weatherCall.enqueue(new Callback<WeatherResponse>(){

            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if(requestId!=currentRequestId||call.isCanceled()){
                    return;
                }
                if(response.isSuccessful()&&response.body()!=null){
                    WeatherResponse weatherData = response.body();
                    if(weatherData.now!=null){
                        callback.onSuccess(cityName,weatherData.now);
                    }else {
                        callback.onError("天气数据解析为空");
                    }
                }else {
                    callback.onError("获取天气失败,HTTP错误码:"+response.code());
                }
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                if(requestId!=currentRequestId||call.isCanceled()){
                    return;
                }
                callback.onError("网络请求异常:"+t.getMessage());
            }
        });
    }

    //取消未完成的请求并让旧请求失效
    public void cancelCurrentRequests(){
        currentRequestId++;
        if(cityCall!=null&&!cityCall.isCanceled()){
            cityCall.cancel();
            cityCall=null;
        }
        if(weatherCall!=null&&!weatherCall.isCanceled()){
            weatherCall.cancel();
            weatherCall=null;
        }
    }
}

















