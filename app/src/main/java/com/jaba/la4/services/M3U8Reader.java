package com.jaba.la4.services;

import android.util.Log;


import com.jaba.la4.models.Chan;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8Reader {
    private static final String TAG = "M3_READER";
    public static List<Chan> chan_list = new ArrayList<>();

    public static void read_M3_async() {

        chan_list.clear();

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            List<Chan> c_list = new ArrayList<>();
            try {
                int index = 0;
                URL url = new URL(InitSettings.h_playlist);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                String name = null;
                String ico = null;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#EXTINF")) {
                        name = extractName(line);
                        ico = extractLogo(line);
                    } else if (!line.startsWith("#") && !line.isEmpty()) {
                        index++;
                        c_list.add(new Chan(name != null ? name : "Unknown", line, ico != null ? ico : "No Logo", index));
                    }
                }
                reader.close();
                chan_list = c_list;

            } catch (Exception e) {
                Log.e(TAG, "ERROR: " + e.getMessage(), e);
            } finally {
                service.shutdown();
            }
        });
    }

    private static String extractName(String line) {
        Pattern pattern = Pattern.compile(",(.*?)$");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    private static String extractLogo(String line) {
        Pattern pattern = Pattern.compile("tvg-logo=\"(.*?)\"");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "No Logo";
    }
}
