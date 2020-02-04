package com.rayworks.droidweekly;

import android.app.Application;

import timber.log.Timber;

public class App extends Application {

    private static App app;

    public static App getApp() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        if(BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
