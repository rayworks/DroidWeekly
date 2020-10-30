package com.rayworks.droidweekly;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import timber.log.Timber;

public class App extends Application {

    private static App app;

    public static App get() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        applyTheme();
    }


    public void updateTheme(ThemeOption option, boolean updatedNow) {
        switch (option) {
            case Day:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Night:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case System:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        if (updatedNow) {
            SharedPreferences pref = getSharedPreferences("app_theme", MODE_PRIVATE);
            pref.edit().putString("theme", option.getValue()).apply();
        }
    }

    public void applyTheme() {
        SharedPreferences pref = getSharedPreferences("app_theme", MODE_PRIVATE);
        String theme = pref.getString("theme", null);
        if (theme != null) {
            updateTheme(ThemeOption.from(theme), false);

        }
    }
}
