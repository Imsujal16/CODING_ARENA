package com.arena.app.network;

import android.util.Log;

import com.arena.app.utils.Constants;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;

    private SocketManager() {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;
            socket = IO.socket(Constants.SOCKET_URL, options);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        }
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
            Log.d(TAG, "Socket connecting...");
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            Log.d(TAG, "Socket disconnected");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public void emit(String event, JSONObject data) {
        if (socket != null) {
            socket.emit(event, data);
            Log.d(TAG, "Emitting: " + event);
        }
    }

    public void on(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    public void off(String event) {
        if (socket != null) {
            socket.off(event);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
