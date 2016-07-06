package com.fernandobarillas.linkshare.configuration;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by fb on 2/1/16.
 */
public class AppPreferences {
    private static final String KEY_API_URL    = "api_url";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USERNAME   = "username";
    private SharedPreferences mPreferences;

    public AppPreferences(Context context) {
        mPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }

    public URL getApiUrl() {
        try {
            return new URL(mPreferences.getString(KEY_API_URL, null));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void setApiUrl(String apiUrl) {
        mPreferences.edit().putString(KEY_API_URL, apiUrl).apply();
    }

    public String getAuthToken() {
        return mPreferences.getString(KEY_AUTH_TOKEN, null);
    }

    public void setAuthToken(String authToken) {
        mPreferences.edit().putString(KEY_AUTH_TOKEN, authToken).apply();
    }

    public String getUsername() {
        return mPreferences.getString(KEY_USERNAME, null);
    }

    public void setUsername(String username) {
        mPreferences.edit().putString(KEY_USERNAME, username).apply();
    }
}
