package com.reel2real.backend.weather;

public enum WeatherType {
    CLEAR,
    CLOUDS,
    RAIN,
    SNOW,
    EXTREME;

    public static WeatherType from(String value) {
        try {
            return WeatherType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return CLEAR;
        }
    }
}
