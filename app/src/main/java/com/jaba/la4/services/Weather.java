package com.jaba.la4.services;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Weather {
    private static final String ERROR = "WEATHER ERROR !!!";

    // Callback interface to deliver weather data
    public interface WeatherCallback {
        void onWeatherReceived(String temp, String wText);
    }

    // Async weather fetcher
    public static void get_weather(WeatherCallback callback) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                // Replace with your actual weather JSON URL
                URL url = new URL(InitSettings.h_weather_val);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                // Parse JSON
                JSONObject obj = new JSONObject(sb.toString());
                String temp = obj.getString("temp_c");
                String wText = obj.getString("w_text");

                // Return the result through callback
                callback.onWeatherReceived(temp, wText);
            } catch (Exception e) {
                Log.i(ERROR, e.toString());
                callback.onWeatherReceived(null, null); // Notify error
            }
        });
    }
}
