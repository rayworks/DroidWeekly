package com.rayworks.droidweekly.model;

public enum ThemeOption {
    Day("day"),
    Night("night"),
    System("system");

    private final String value;

    public String getValue() {
        return value;
    }

    ThemeOption(String opt) {
        value = opt;
    }

    public static ThemeOption from(String value) {
        for (ThemeOption opt : ThemeOption.values()) {
            if (opt.value.equals(value)) {
                return opt;
            }
        }
        return System;
    }
}
