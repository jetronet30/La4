package com.jaba.la4.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InitSettings {

    // Config values
    public static String ip = "192.168.1.50";
    public static int port = 8089;
    public static int room = 0;
    public static boolean vod = false;
    public static boolean control = false;
    public static boolean hotel = false;
    public static boolean alarm_type = false;
    public static String alarm_server_ip = "192.168.1.50";
    public static int alarm_port = 3434;
    public static boolean only_live = false;

    // URLs
    public static String h_playlist;
    public static String h_weather_img;
    public static String h_wifi_qr_img;
    public static String h_bac_video;
    public static String h_weather_val;

    // Status flags
    public static boolean tv_status = false;
    public static boolean alarm_status = false;

    private static final String FILE_NAME = "settings.json";
    private static final String TAG = "InitSettings";

    public static void init_settings(Context context) {
        File settingsFile = new File(context.getFilesDir(), FILE_NAME);
        if (!settingsFile.exists()) {
            write_settings(settingsFile); // Writes default values
        } else {
            read_settings(settingsFile);
        }
        init_h_links();
        check_status_tv();
    }

    public static void set_settings(Context context,
                                    String ip_s, int port_s, int room_s,
                                    boolean vod_s, boolean control_s, boolean hotel_s,
                                    boolean alarm_type_s, String alarm_server_ip_s,
                                    int alarm_port_s, boolean only_live_s) {

        // Assign new settings
        ip = ip_s;
        port = port_s;
        room = room_s;
        vod = vod_s;
        control = control_s;
        hotel = hotel_s;
        alarm_type = alarm_type_s;
        alarm_server_ip = alarm_server_ip_s;
        alarm_port = alarm_port_s;
        only_live = only_live_s;

        File settingsFile = new File(context.getFilesDir(), FILE_NAME);
        write_settings(settingsFile);
        init_h_links();
        check_status_tv();
    }

    private static void write_settings(File file) {
        try {
            JSONObject json = new JSONObject();
            json.put("ip", ip);
            json.put("port", port);
            json.put("room", room);
            json.put("vod", vod);
            json.put("control", control);
            json.put("hotel", hotel);
            json.put("alarm_type", alarm_type);
            json.put("alarm_server_ip", alarm_server_ip);
            json.put("alarm_port", alarm_port);
            json.put("only_live", only_live);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString(4));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error writing settings: ", e);
        }
    }

    private static void read_settings(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject json = new JSONObject(builder.toString());

            ip = json.optString("ip", ip);
            port = json.optInt("port", port);
            room = json.optInt("room", room);
            vod = json.optBoolean("vod", vod);
            control = json.optBoolean("control", control);
            hotel = json.optBoolean("hotel", hotel);
            alarm_type = json.optBoolean("alarm_type", alarm_type);
            alarm_server_ip = json.optString("alarm_server_ip", alarm_server_ip);
            alarm_port = json.optInt("alarm_port", alarm_port);
            only_live = json.optBoolean("only_live", only_live);

        } catch (Exception e) {
            Log.e(TAG, "Error reading settings: ", e);
        }
    }

    private static void init_h_links() {
        String baseUrl = "http://" + ip + ":" + port;
        h_playlist = baseUrl + "/playlist/" + room;
        h_weather_img = baseUrl + "/weather_img";
        h_wifi_qr_img = baseUrl + "/wifi_qr";
        h_bac_video = baseUrl + "/bacvideo";
        h_weather_val = baseUrl + "/weather_values";
    }

    private static void check_status_tv() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean result = false;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(h_playlist);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.connect();
                result = (connection.getResponseCode() == 200);
            } catch (Exception e) {
                Log.e(TAG, "TV connection error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                tv_status = result;
                executor.shutdown();
            }
        });
    }
}
