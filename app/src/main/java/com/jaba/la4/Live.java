package com.jaba.la4;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.UdpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jaba.la4.models.Chan;
import com.jaba.la4.services.AlarmService;
import com.jaba.la4.services.InitSettings;
import com.jaba.la4.services.M3U8Reader;

public class Live extends FragmentActivity {

    private static int index = 1;

    private TextView chan_num, select_num;
    private LinearLayout chan_bar, chan_scroll;
    private ConstraintLayout alarmView;
    private PlayerView playerView;
    private ExoPlayer player;

    private boolean isChanBarVisible = false;

    private final Handler handler = new Handler();
    private final Handler chanBarHandler = new Handler();
    private final Handler numberInputHandler = new Handler();
    private final Handler handlerSettingsAct = new Handler();

    private final StringBuilder numberInput = new StringBuilder();
    private final StringBuilder keyBuffer = new StringBuilder();

    private Runnable timeoutSettingAct;
    private final Runnable hideChanNumRunnable = () -> chan_num.setVisibility(View.INVISIBLE);
    private final Runnable hideChanBarRunnable = () -> {
        chan_bar.setVisibility(View.INVISIBLE);
        isChanBarVisible = false;
    };

    private final Runnable numberInputRunnable = this::handleNumberInput;

    private int lastKeyPressed = -1;

    @SuppressLint("WrongViewCast")
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InitSettings.init_settings(this);
        M3U8Reader.read_M3_async();

        setContentView(R.layout.activity_live);
        initViews();

        alarmView = findViewById(R.id.alarm);
        AlarmService.startListening(message -> runOnUiThread(() -> {
            if ("ALARM".equalsIgnoreCase(message)) {
                alarmView.setVisibility(View.VISIBLE);
            } else if ("NORMAL".equalsIgnoreCase(message)) {
                alarmView.setVisibility(View.GONE);
            }
        }));

        new Handler().postDelayed(() -> {
            Chan chan = get_by_index(index);
            if (chan != null) {
                playStream(chan.getUrl());
                showChannelNumber();
            }
        }, 1000);
    }

    private void initViews() {
        playerView = findViewById(R.id.player_live);
        playerView.setKeepScreenOn(true);

        chan_num = findViewById(R.id.chan_num);
        select_num = findViewById(R.id.select_num);
        chan_bar = findViewById(R.id.chan_bar);
        chan_scroll = findViewById(R.id.chan_scroll);

        chan_num.setVisibility(View.INVISIBLE);
        select_num.setVisibility(View.INVISIBLE);
        select_num.setText(String.valueOf(index));

        timeoutSettingAct = () -> {
            lastKeyPressed = -1;
            keyBuffer.setLength(0);
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (M3U8Reader.chan_list == null || M3U8Reader.chan_list.isEmpty()) return false;

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isChanBarVisible) {
                chan_bar.setVisibility(View.INVISIBLE);
                isChanBarVisible = false;
                return true;
            } else {
                Intent intent = new Intent(Live.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
        }

        if (isChanBarVisible) {
            // Allow D-pad navigation inside chan_bar when open
            return super.onKeyDown(keyCode, event);
        }

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            numberInput.append(keyCode - KeyEvent.KEYCODE_0);
            select_num.setText(numberInput.toString());
            select_num.setVisibility(View.VISIBLE);
            numberInputHandler.removeCallbacks(numberInputRunnable);
            numberInputHandler.postDelayed(numberInputRunnable, 1500);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_INFO) {
            if (lastKeyPressed == KeyEvent.KEYCODE_INFO) {
                startActivity(new Intent(this, SettingsAct.class));
                keyBuffer.setLength(0);
            } else {
                lastKeyPressed = KeyEvent.KEYCODE_INFO;
                resetTimer();
            }
            return true;
        }

        Chan chan = null;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
            live_up();
            chan = get_by_index(index);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            live_down();
            chan = get_by_index(index);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            toggleChanBarVisibility();
            return true;
        }

        if (chan != null) {
            playStream(chan.getUrl());
            showChannelNumber();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void resetTimer() {
        handlerSettingsAct.removeCallbacks(timeoutSettingAct);
        handlerSettingsAct.postDelayed(timeoutSettingAct, 3000);
    }

    private void handleNumberInput() {
        if (numberInput.length() == 0) return;

        try {
            int selectedIndex = Integer.parseInt(numberInput.toString());
            Chan selectedChan = get_by_index(selectedIndex);
            if (selectedChan != null) {
                index = selectedIndex;
                playStream(selectedChan.getUrl());
                showChannelNumber();
            }
        } catch (NumberFormatException ignored) {
        }

        numberInput.setLength(0);
        select_num.setVisibility(View.INVISIBLE);
    }

    private void showChannelNumber() {
        chan_num.setText(String.valueOf(index));
        chan_num.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideChanNumRunnable);
        handler.postDelayed(hideChanNumRunnable, 4000);
    }

    private void toggleChanBarVisibility() {
        chan_scroll.removeAllViews();

        for (Chan ch : M3U8Reader.chan_list) {
            ImageButton imgButton = new ImageButton(this);
            imgButton.setImageResource(R.drawable.icon_api);
            imgButton.setScaleType(ImageView.ScaleType.CENTER_CROP);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(40, 10, 10, 10);
            imgButton.setLayoutParams(params);
            imgButton.setBackgroundColor(Color.TRANSPARENT);
            imgButton.setAdjustViewBounds(true);

            imgButton.setOnFocusChangeListener(new FocusHighlighter());

            imgButton.setOnClickListener(v -> {
                index = ch.getIndex();
                playStream(ch.getUrl());
                showChannelNumber();
                chan_bar.setVisibility(View.INVISIBLE);
                isChanBarVisible = false;
            });

            if (ch.getIndex() == index) {
                imgButton.setSelected(true);
                imgButton.requestFocus();
            }

            Glide.with(this)
                    .load(ch.getIcon())
                    .apply(new RequestOptions().error(R.drawable.icon_api).override(100, 90))
                    .into(imgButton);

            chan_scroll.addView(imgButton);
        }

        isChanBarVisible = !isChanBarVisible;
        chan_bar.setVisibility(isChanBarVisible ? View.VISIBLE : View.INVISIBLE);

        chanBarHandler.removeCallbacks(hideChanBarRunnable);
        if (isChanBarVisible) {
            chanBarHandler.postDelayed(hideChanBarRunnable, 10000);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playStream(@NonNull String streamUri) {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(2000, 10000, 1500, 1000)
                .build();

        if (player == null) {
            player = new ExoPlayer.Builder(this).setLoadControl(loadControl).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e("PlayerError", "Playback error: " + error.getMessage());
                }
            });
        } else {
            player.stop();
            player.clearMediaItems();
        }

        MediaSource mediaSource;
        if (streamUri.startsWith("http://") || streamUri.startsWith("https://")) {
            mediaSource = new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(streamUri));
        } else if (streamUri.startsWith("udp://@")) {
            mediaSource = new ProgressiveMediaSource.Factory(UdpDataSource::new)
                    .createMediaSource(MediaItem.fromUri(streamUri));
        } else {
            Log.e("StreamError", "Unsupported stream format: " + streamUri);
            return;
        }

        player.setMediaSource(mediaSource);
        player.setPlayWhenReady(true);
        player.prepare();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.setPlayWhenReady(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private Chan get_by_index(int index) {
        for (Chan chan : M3U8Reader.chan_list) {
            if (chan.getIndex() == index) return chan;
        }
        return null;
    }

    private void live_up() {
        if (!M3U8Reader.chan_list.isEmpty()) {
            index++;
            if (index > M3U8Reader.chan_list.size()) index = 1;
        }
    }

    private void live_down() {
        if (!M3U8Reader.chan_list.isEmpty()) {
            index--;
            if (index < 1) index = M3U8Reader.chan_list.size();
        }
    }

    private static class FocusHighlighter implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            view.setBackgroundColor(hasFocus ? Color.parseColor("#AA3D47AE") : Color.TRANSPARENT);
        }
    }
}
