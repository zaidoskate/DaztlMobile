package com.example.daztlmobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "daztl_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String fetchToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
