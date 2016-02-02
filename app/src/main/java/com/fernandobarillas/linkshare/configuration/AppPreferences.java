package com.fernandobarillas.linkshare.configuration;

import android.content.Context;

import com.securepreferences.SecurePreferences;

/**
 * Created by fb on 2/1/16.
 */
public class AppPreferences {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private SecurePreferences mSecurePreferences;

    public AppPreferences(Context context) {
        SecurePreferences.setLoggingEnabled(true);
        mSecurePreferences = new SecurePreferences(context);
    }

    public String getPassword() {
        return mSecurePreferences.getString(KEY_PASSWORD, null);
    }

    public void setPassword(String password) {
        mSecurePreferences.edit().putString(KEY_PASSWORD, password).apply();
    }

    public String getRefreshToken() {
        return mSecurePreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public void setRefreshToken(String refreshToken) {
        mSecurePreferences.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply();
    }

    public String getUsername() {
        return mSecurePreferences.getString(KEY_USERNAME, null);
    }

    public void setUsername(String username) {
        mSecurePreferences.edit().putString(KEY_USERNAME, username).apply();
    }
}
