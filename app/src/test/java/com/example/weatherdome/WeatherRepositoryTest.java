package com.example.weatherdome;

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

public class WeatherRepositoryTest {
    private final ManualService service = new ManualService();
    private final WeatherRepository repository = new WeatherRepository(service);
    private final RecordingCallback results = new RecordingCallback();

    @Test
    public void lateCityResponseFromA_doesNotStartWeatherAfterB() {
        repository.getWeatherByCity("A", results);
        ManualCall<GeoResponse> cityA = service.cities.get(0);
        // 模拟结果已排队、取消无法拦住回调，单独验证 requestId 防护。
        cityA.reportCancellation = false;
        repository.getWeatherByCity("B", results);
        assertTrue(cityA.cancelRequested);

        service.cities.get(1).respond(city("B-id"));
        service.weathers.get(0).respond(weather("20"));
        cityA.respond(city("A-id"));

        assertEquals(Collections.singletonList("B-id"), service.weatherLocations);
        assertEquals(Collections.singletonList("B:20"), results.successes);
        assertTrue(results.errors.isEmpty());
    }

    @Test
    public void lateWeatherSuccessFromA_doesNotOverwriteB() {
        ManualCall<WeatherResponse> weatherA = startWeatherAThenB();
        service.cities.get(1).respond(city("B-id"));
        service.weathers.get(1).respond(weather("20"));
        weatherA.respond(weather("10"));

        assertEquals(Collections.singletonList("B:20"), results.successes);
        assertTrue(results.errors.isEmpty());
    }

    @Test
    public void lateFailuresFromA_doNotReportErrorAfterB() {
        ManualCall<WeatherResponse> weatherA = startWeatherAThenB();
        service.cities.get(1).respond(city("B-id"));
        service.weathers.get(1).respond(weather("20"));
        weatherA.fail(new IOException("late A failure"));

        assertEquals(Collections.singletonList("B:20"), results.successes);
        assertTrue(results.errors.isEmpty());
    }

    @Test
    public void explicitCancel_ignoresLateCityFailureAndWeatherSuccess() {
        repository.getWeatherByCity("A", results);
        ManualCall<GeoResponse> cityA = service.cities.get(0);
        cityA.respond(city("A-id"));
        ManualCall<WeatherResponse> weatherA = service.weathers.get(0);

        repository.cancelCurrentRequests();
        assertTrue(cityA.cancelRequested);
        assertTrue(weatherA.cancelRequested);
        // 即便取消后仍有回调到达，也不能向外发布结果。
        cityA.fail(new IOException("Canceled"));
        weatherA.respond(weather("10"));

        assertTrue(results.successes.isEmpty());
        assertTrue(results.errors.isEmpty());
    }

    @Test
    public void currentRequestFailure_isReported() {
        repository.getWeatherByCity("A", results);
        service.cities.get(0).fail(new IOException("offline"));

        assertTrue(results.successes.isEmpty());
        assertEquals(Collections.singletonList("网络请求异常:offline"), results.errors);
        assertTrue(service.weathers.isEmpty());
    }

    private ManualCall<WeatherResponse> startWeatherAThenB() {
        repository.getWeatherByCity("A", results);
        service.cities.get(0).respond(city("A-id"));
        ManualCall<WeatherResponse> weatherA = service.weathers.get(0);
        weatherA.reportCancellation = false;
        repository.getWeatherByCity("B", results);
        assertTrue(weatherA.cancelRequested);
        return weatherA;
    }

    private static GeoResponse city(String id) {
        GeoResponse response = new GeoResponse();
        GeoResponse.Location location = new GeoResponse.Location();
        location.id = id;
        response.code = "200";
        response.location = Collections.singletonList(location);
        return response;
    }

    private static WeatherResponse weather(String temperature) {
        WeatherResponse response = new WeatherResponse();
        response.code = "200";
        response.now = new WeatherResponse.NowData();
        response.now.temp = temperature;
        return response;
    }

    private static final class RecordingCallback implements WeatherRepository.WeatherCallback {
        final List<String> successes = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public void onSuccess(String cityName, WeatherResponse.NowData nowData) {
            successes.add(cityName + ":" + nowData.temp);
        }

        @Override
        public void onError(String errorMessage) {
            errors.add(errorMessage);
        }
    }

    private static final class ManualService implements WeatherApiService {
        final List<ManualCall<GeoResponse>> cities = new ArrayList<>();
        final List<ManualCall<WeatherResponse>> weathers = new ArrayList<>();
        final List<String> weatherLocations = new ArrayList<>();

        @Override
        public Call<GeoResponse> getCityId(String cityName, String apiKey) {
            ManualCall<GeoResponse> call = new ManualCall<>();
            cities.add(call);
            return call;
        }

        @Override
        public Call<WeatherResponse> getCurrentWeather(String locationId, String apiKey) {
            ManualCall<WeatherResponse> call = new ManualCall<>();
            weathers.add(call);
            weatherLocations.add(locationId);
            return call;
        }
    }

    // 无线程、无等待、无网络；respond/fail 由测试显式调用。
    private static final class ManualCall<T> implements Call<T> {
        private Callback<T> callback;
        boolean cancelRequested;
        boolean reportCancellation = true;

        void respond(T body) {
            assertNotNull("请求必须先 enqueue", callback);
            callback.onResponse(this, Response.success(body));
        }

        void fail(Throwable failure) {
            assertNotNull("请求必须先 enqueue", callback);
            callback.onFailure(this, failure);
        }

        @Override
        public void enqueue(Callback<T> callback) {
            if (this.callback != null) throw new IllegalStateException("Already executed");
            this.callback = callback;
        }

        @Override
        public void cancel() { cancelRequested = true; }

        @Override
        public boolean isCanceled() { return reportCancellation && cancelRequested; }

        @Override
        public boolean isExecuted() { return callback != null; }

        @Override
        public Response<T> execute() {
            throw new UnsupportedOperationException("测试仅使用异步回调");
        }

        @Override
        public Call<T> clone() { return new ManualCall<>(); }

        @Override
        public Request request() {
            return new Request.Builder().url("https://example.invalid/").build();
        }

        @Override
        public Timeout timeout() { return Timeout.NONE; }
    }
}
