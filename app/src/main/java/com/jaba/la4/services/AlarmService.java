package com.jaba.la4.services;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AlarmService {
    private static final int BUFFER_SIZE = 1024;
    private static boolean isRunning = false;
    private static DatagramSocket socket;
    private static MessageListener currentListener;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public static void startListening(MessageListener listener) {
        currentListener = listener;
        if (isRunning) return;

        isRunning = true;
        new Thread(() -> {
            try {
                socket = new DatagramSocket(InitSettings.alarm_port);
                byte[] buffer = new byte[BUFFER_SIZE];

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    Log.d("AlarmService", "Received message: " + message);
                    if (currentListener != null) {
                        currentListener.onMessageReceived(message);
                    }
                }

            } catch (Exception e) {
                if (isRunning) {
                    Log.e("AlarmService", "Error in AlarmService: " + e.getMessage(), e);
                } else {
                    Log.d("AlarmService", "Socket closed normally.");
                }
            }
        }).start();
    }

    public static void stopListening() {
        isRunning = false;
        currentListener = null;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
