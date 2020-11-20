package com.rayworks.droidweekly;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

import com.rayworks.droidweekly.model.ThemeOption;

import timber.log.Timber;

public class App extends Application {

    private static App app;
    private SharedPreferences pref;

    public static App get() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        pref = getSharedPreferences("app_theme", MODE_PRIVATE);

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
            pref.edit().putString("theme", option.getValue()).apply();
        }
    }

    public void applyTheme() {
        String theme = pref.getString("theme", null);
        if (theme != null) {
            updateTheme(ThemeOption.from(theme), false);

        }
    }

    public void saveLocalAvatar(Uri uri) {
        pref.edit().putString("uri_avatar", uri.toString()).apply();
    }

    public String getLocalAvatar() {
        return pref.getString("uri_avatar", "");
    }
}
