package com.arena.app;

import android.app.Application;

import com.arena.app.auth.ClerkSessionBridge;
import com.arena.app.utils.ClerkAuthHelper;

public class ArenaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (!new ClerkAuthHelper(this).isGuestModeEnabled()) {
            ClerkSessionBridge.ensureInitialized(this);
            ClerkSessionBridge.syncSessionAsync(this);
        }
    }
}
