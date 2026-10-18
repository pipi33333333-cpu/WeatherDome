package com.example.weatherdome;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class WeatherViewModelTest {
    private final CountingService service = new CountingService();
    private WeatherViewModel viewModel;

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        viewModel = new WeatherViewModel(new WeatherRepository(service));
    }

    @After
    public void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    public void blankInputs_startNoRequests() {
        viewModel.loadWeather(null);
        viewModel.loadWeather("");
        viewModel.loadWeather("   ");
        assertCounts(0, 0);
    }

    @Test
    public void duplicateDuringCityLookup_keepsOriginalRequestAndTrimsCity() {
        viewModel.loadWeather(" 北京 ");
        viewModel.loadWeather("北京");
        viewModel.loadWeather(" 北京 ");
        assertEquals(Collections.singletonList("北京"), service.cityNames);
        assertCounts(1, 0);
        assertFalse(service.cities.get(0).isCanceled());
        respondCity(0);
        assertCounts(1, 1);
    }

    @Test
    public void duplicateDuringWeatherRequest_keepsOriginalRequest() {
        viewModel.loadWeather("北京");
        respondCity(0);
        viewModel.loadWeather(" 北京 ");
        assertCounts(1, 1);
        assertFalse(service.weathers.get(0).isCanceled());
    }

    @Test
    public void cityNetworkFailure_allowsSameCityRetry() {
        viewModel.loadWeather("北京");
        service.cities.get(0).fail();
        assertEquals(WeatherUiState.Status.ERROR, viewModel.getUistate().getValue().getStatus());
        viewModel.loadWeather(" 北京 ");
        assertCounts(2, 0);
        respondCity(1);
        assertCounts(2, 1);
    }

    @Test
    public void weatherNetworkFailure_allowsSameCityRetry() {
        viewModel.loadWeather("北京");
        respondCity(0);
        service.weathers.get(0).fail();
        viewModel.loadWeather("北京");
        respondCity(1);
        assertCounts(2, 2);
    }

    @Test
    public void emptyCityResponse_allowsSameCityRetry() {
        viewModel.loadWeather("北京");
        service.cities.get(0).respond(new GeoResponse());
        viewModel.loadWeather("北京");
        assertCounts(2, 0);
    }

    @Test
    public void success_allowsSameCityRefresh() {
        viewModel.loadWeather("北京");
        respondCity(0);
        WeatherResponse response = new WeatherResponse();
        response.now = new WeatherResponse.NowData();
        service.weathers.get(0).respond(response);
        assertEquals(WeatherUiState.Status.SUCCESS, viewModel.getUistate().getValue().getStatus());
        viewModel.loadWeather("北京");
        respondCity(1);
        assertCounts(2, 2);
    }

    @Test
    public void blankCancelsRequest_andSameCityCanStartAgain() {
        viewModel.loadWeather("北京");
        viewModel.loadWeather(" ");
        assertCounts(1, 0);
        assertTrue(service.cities.get(0).isCanceled());
        viewModel.loadWeather("北京");
        assertCounts(2, 0);
    }

    @Test
    public void differentCityCancelsOldRequest_lateCallbackDoesNotUnlockNewRequest() {
        viewModel.loadWeather("北京");
        respondCity(0);
        viewModel.loadWeather("上海");
        assertTrue(service.weathers.get(0).isCanceled());
        service.weathers.get(0).fail();
        viewModel.loadWeather(" 上海 ");
        assertCounts(2, 1);
        assertFalse(service.cities.get(1).isCanceled());
    }

    private void respondCity(int index) {
        GeoResponse response = new GeoResponse();
        GeoResponse.Location location = new GeoResponse.Location();
        location.id = "city-id";
        response.location = Collections.singletonList(location);
        service.cities.get(index).respond(response);
    }

    private void assertCounts(int cities, int weathers) {
        assertEquals("城市接口请求数", cities, service.cities.size());
        assertEquals("天气接口请求数", weathers, service.weathers.size());
    }

    private static final class CountingService implements WeatherApiService {
        final List<String> cityNames = new ArrayList<>();
        final List<ManualCall<GeoResponse>> cities = new ArrayList<>();
        final List<ManualCall<WeatherResponse>> weathers = new ArrayList<>();

        @Override
        public Call<GeoResponse> getCityId(String cityName, String apiKey) {
            cityNames.add(cityName);
            ManualCall<GeoResponse> call = new ManualCall<>();
            cities.add(call);
            return call;
        }

        @Override
        public Call<WeatherResponse> getCurrentWeather(String locationId, String apiKey) {
            ManualCall<WeatherResponse> call = new ManualCall<>();
            weathers.add(call);
            return call;
        }
    }

    // 手动控制响应时机，分别统计两个接口；不访问真实网络。
    private static final class ManualCall<T> implements Call<T> {
        private Callback<T> callback;
        private boolean canceled;

        void respond(T body) { callback.onResponse(this, Response.success(body)); }
        void fail() { callback.onFailure(this, new IOException("offline")); }

        @Override public void enqueue(Callback<T> callback) {
            if (this.callback != null) throw new IllegalStateException("Already executed");
            this.callback = callback;
        }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public boolean isExecuted() { return callback != null; }
        @Override public Response<T> execute() { throw new UnsupportedOperationException(); }
        @Override public Call<T> clone() { return new ManualCall<>(); }
        @Override public Request request() {
            return new Request.Builder().url("https://example.invalid/").build();
        }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }
}
