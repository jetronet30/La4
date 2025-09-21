package com.jaba.la4;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jaba.la4.services.AlarmService;
import com.jaba.la4.services.InitSettings;
import com.jaba.la4.services.Weather;

public class MainActivity extends FragmentActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    private final Handler handlerSettingsAct = new Handler();
    private Runnable timeoutSettingAct;
    private final StringBuilder keyBuffer = new StringBuilder();

    private final Handler weatherHandler = new Handler();
    private Runnable weatherRunnable;

    private final Handler inactivityHandler = new Handler();
    private Runnable hideUiRunnable;
    private static final int INACTIVITY_TIMEOUT = 20 * 60 * 1000; // 20 minutes

    private TextView alarmView;
    private TextView weaText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InitSettings.init_settings(this);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Started");

        startLockTaskOptimized();

        if (InitSettings.only_live) {
            Intent intent = new Intent(this, Live.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        alarmView = findViewById(R.id.alarm);
        weaText = findViewById(R.id.wea_text);

        fetchAndUpdateWeather();
        updateWeatherImage();
        scheduleWeatherUpdates();

        ImageView wifiQrImg = findViewById(R.id.wifi_qr_img);
        if (wifiQrImg != null && InitSettings.h_wifi_qr_img != null && !InitSettings.h_wifi_qr_img.isEmpty()) {
            Glide.with(this)
                    .load(InitSettings.h_wifi_qr_img)
                    .placeholder(R.drawable.icon_api)
                    .error(R.drawable.icon_api)
                    .into(wifiQrImg);
        }

        playerView = findViewById(R.id.player);
        initializePlayer();

        timeoutSettingAct = () -> {
            if ("1657165".equals(keyBuffer.toString())) {
                startActivity(new Intent(this, SettingsAct.class));
            }
            keyBuffer.setLength(0);
        };

        hideUiRunnable = () -> setUiVisibility(false);
        resetInactivityTimer();

        setupButtons();

        Button live = findViewById(R.id.live);
        Button youtube = findViewById(R.id.youtube);

        live.setOnClickListener(v -> {
            Intent intent = new Intent(this, Live.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        youtube.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube.tv");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "YouTube TV app not installed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startLockTaskOptimized() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.setLockTaskPackages(adminComponent, new String[]{
                    getPackageName(),
                    "com.android.tv.settings",
                    "com.google.android.youtube.tv"
            });

            // ასინქრონული LockTask სტარტი UI Thread-ის არ დაბლოკვისთვის
            new Handler().post(() -> {
                try {
                    if (!isInLockTaskMode()) {
                        startLockTask();
                        Log.d("MainActivity", "LockTask started (kiosk mode) async");
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to start LockTask", e);
                }
            });
        }
    }

    private boolean isInLockTaskMode() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
        } else {
            return am.isInLockTaskMode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmService.startListening(message -> runOnUiThread(() -> {
            if ("ALARM".equalsIgnoreCase(message)) {
                alarmView.setVisibility(View.VISIBLE);
            } else if ("NORMAL".equalsIgnoreCase(message)) {
                alarmView.setVisibility(View.GONE);
            }
        }));
    }

    @SuppressLint("SetTextI18n")
    private void fetchAndUpdateWeather() {
        Weather.get_weather((temp, wText) -> runOnUiThread(() -> {
            if (temp != null && weaText != null) {
                weaText.setText(temp + "°C");
            } else if (weaText != null) {
                weaText.setText("Weather Unavailable");
            }
        }));
    }

    private void updateWeatherImage() {
        ImageView weaImg = findViewById(R.id.wea_img);
        if (weaImg != null && InitSettings.h_weather_img != null && !InitSettings.h_weather_img.isEmpty()) {
            Glide.with(this)
                    .load(InitSettings.h_weather_img)
                    .placeholder(R.drawable.icon_api)
                    .error(R.drawable.icon_api)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(weaImg);
        }
    }

    private void scheduleWeatherUpdates() {
        if (weatherRunnable == null) {
            weatherRunnable = new Runnable() {
                @Override
                public void run() {
                    fetchAndUpdateWeather();
                    updateWeatherImage();
                    weatherHandler.postDelayed(this, 60 * 60 * 1000); // ყოველ 1 საათში
                }
            };
        }
        weatherHandler.post(weatherRunnable);
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(InitSettings.h_bac_video);
        //MediaItem mediaItem = MediaItem.fromUri("https://hostv.ge/downloads/bac.mp4");
        player.setMediaItem(mediaItem);
        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        player.prepare();
        player.play();
    }

    private void setupButtons() {
        Button live = findViewById(R.id.live);
        Button vod = findViewById(R.id.vod);
        Button hotel = findViewById(R.id.hotel);
        Button youtube = findViewById(R.id.youtube);
        Button control = findViewById(R.id.control);
        Button taxi = findViewById(R.id.taxi);

        vod.setVisibility(InitSettings.vod ? View.VISIBLE : View.GONE);
        control.setVisibility(InitSettings.control ? View.VISIBLE : View.GONE);
        hotel.setVisibility(InitSettings.hotel ? View.VISIBLE : View.GONE);

        View.OnFocusChangeListener focusEffect = new FocusEffect();
        taxi.setOnFocusChangeListener(focusEffect);
        live.setOnFocusChangeListener(focusEffect);
        vod.setOnFocusChangeListener(focusEffect);
        hotel.setOnFocusChangeListener(focusEffect);
        youtube.setOnFocusChangeListener(focusEffect);
        control.setOnFocusChangeListener(focusEffect);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            resetInactivityTimer();

            if (keyCode == KeyEvent.KEYCODE_HOME) {
                return true;
            }

            keyBuffer.append(keyCode);
            if (keyBuffer.length() > 10) {
                keyBuffer.delete(0, keyBuffer.length() - 10);
            }
            resetTimer();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void resetTimer() {
        handlerSettingsAct.removeCallbacks(timeoutSettingAct);
        handlerSettingsAct.postDelayed(timeoutSettingAct, 3000);
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(hideUiRunnable);
        setUiVisibility(true);
        inactivityHandler.postDelayed(hideUiRunnable, INACTIVITY_TIMEOUT);
    }

    private void setUiVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;

        findViewById(R.id.textClock).setVisibility(visibility);
        findViewById(R.id.wea_img).setVisibility(visibility);
        findViewById(R.id.wea_text).setVisibility(visibility);
        findViewById(R.id.wifi_qr_img).setVisibility(visibility);
        findViewById(R.id.buttons_line).setVisibility(visibility);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player == null) {
            initializePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerSettingsAct.removeCallbacks(timeoutSettingAct);
        weatherHandler.removeCallbacks(weatherRunnable);
        inactivityHandler.removeCallbacks(hideUiRunnable);
    }

    private static class FocusEffect implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            v.setBackgroundColor(Color.parseColor(hasFocus ? "#B729782F" : "#185C7C5D"));
        }
    }
}
