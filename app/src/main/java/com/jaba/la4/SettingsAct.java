package com.jaba.la4;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.jaba.la4.services.InitSettings;

public class SettingsAct extends FragmentActivity {
    @SuppressLint({"UseSwitchCompatOrMaterialCode", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button set = findViewById(R.id.set);
        Button go = findViewById(R.id.go);
        Button tvSettings = findViewById(R.id.tv_settings);
        EditText ip = findViewById(R.id.ip);
        EditText port = findViewById(R.id.port);
        EditText room = findViewById(R.id.room);
        EditText alarm_server_ip = findViewById(R.id.alarm_server_ip);
        EditText alarm_port = findViewById(R.id.alarm_port);
        Switch vod = findViewById(R.id.vod);
        Switch control = findViewById(R.id.control);
        Switch hotel = findViewById(R.id.hotel);
        Switch alarm_type = findViewById(R.id.alarm_type);
        Switch only_live = findViewById(R.id.only_live);

        TextView tv_status = findViewById(R.id.tv_status);
        if (InitSettings.tv_status) {
            tv_status.setText("connection ok");
            tv_status.setTextColor(Color.parseColor("#05FA4A"));
        } else {
            tv_status.setText("not connection");
            tv_status.setTextColor(Color.parseColor("#FF0000"));
        }

        set.setOnFocusChangeListener(new FocusEffect());
        go.setOnFocusChangeListener(new FocusEffect());
        tvSettings.setOnFocusChangeListener(new FocusEffect());

        ip.setText(InitSettings.ip);
        port.setText(String.valueOf(InitSettings.port));
        room.setText(String.valueOf(InitSettings.room));
        alarm_server_ip.setText(InitSettings.alarm_server_ip);
        alarm_port.setText(String.valueOf(InitSettings.alarm_port));
        vod.setChecked(InitSettings.vod);
        control.setChecked(InitSettings.control);
        hotel.setChecked(InitSettings.hotel);
        alarm_type.setChecked(InitSettings.alarm_type);
        only_live.setChecked(InitSettings.only_live);

        set.setOnClickListener(v -> {
            try {
                String ipValue = ip.getText().toString().trim();
                String alarmIpValue = alarm_server_ip.getText().toString().trim();

                int portValue = parseOrZero(port.getText().toString().trim());
                int roomValue = parseOrZero(room.getText().toString().trim());
                int alarmPortValue = parseOrZero(alarm_port.getText().toString().trim());

                if (ipValue.isEmpty() || alarmIpValue.isEmpty()) {
                    Toast.makeText(this, "IP fields cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                InitSettings.set_settings(this, ipValue, portValue, roomValue,
                        vod.isChecked(), control.isChecked(), hotel.isChecked(),
                        alarm_type.isChecked(), alarmIpValue, alarmPortValue, only_live.isChecked());

                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (InitSettings.tv_status) {
                tv_status.setText("connection ok");
                tv_status.setTextColor(Color.parseColor("#05FA4A"));
            } else {
                tv_status.setText("not connection");
                tv_status.setTextColor(Color.parseColor("#FF0000"));
            }
        });

        // Go button action
        go.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // TV Settings button action
        tvSettings.setOnClickListener(v -> {
            stopLockTask(); // კიოსკიდან გამოსვლა საჭიროების შემთხვევაში

            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setAction(android.provider.Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "TV Settings app not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int parseOrZero(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static class FocusEffect implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            v.setBackgroundColor(Color.parseColor(hasFocus ? "#B729782F" : "#185C7C5D"));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_BACK:
            case android.view.KeyEvent.KEYCODE_HOME:
            case android.view.KeyEvent.KEYCODE_ESCAPE:
            case android.view.KeyEvent.KEYCODE_MENU:
            case android.view.KeyEvent.KEYCODE_SETTINGS:
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
}
