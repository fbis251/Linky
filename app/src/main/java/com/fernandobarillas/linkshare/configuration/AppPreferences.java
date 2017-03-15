package com.fernandobarillas.linkshare.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.fernandobarillas.linkshare.R;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by fb on 2/1/16.
 */
public class AppPreferences {
    public static final int USERNAME_MIN_LENGTH = 2;
    public static final int PASSWORD_MIN_LENGTH = 8;

    private static final String KEY_API_URL               = "api_url";
    private static final String KEY_AUTH_STRING           = "auth_string";
    private static final String KEY_USERNAME              = "username";
    private static final String KEY_USER_ID               = "user_id";
    private static final String KEY_LAST_UPDATE_TIMESTAMP = "last_update_timestamp";

    private static String  sKeyTapCategoryToBrowse;
    private static boolean sDefaultTapCategoryToBrowse;

    private SharedPreferences mPreferences;

    public AppPreferences(Context context) {
        if (context == null) return;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Resources resources = context.getResources();

        // Get preference key strings
        sKeyTapCategoryToBrowse = context.getString(R.string.preference_tap_category_key);

        // Get default preference values
        if (resources != null) {
            sDefaultTapCategoryToBrowse =
                    resources.getBoolean(R.bool.preference_tap_category_default);
        }
    }

    public void deleteAllPreferences() {
        mPreferences.edit().clear().commit();
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

    public String getAuthString() {
        return mPreferences.getString(KEY_AUTH_STRING, null);
    }

    public void setAuthString(String authString) {
        mPreferences.edit().putString(KEY_AUTH_STRING, authString).apply();
    }

    public long getLastUpdateTimestamp() {
        return mPreferences.getLong(KEY_LAST_UPDATE_TIMESTAMP, -1);
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        mPreferences.edit().putLong(KEY_LAST_UPDATE_TIMESTAMP, lastUpdateTimestamp).apply();
    }

    public long getUserId() {
        return mPreferences.getLong(KEY_USER_ID, -1);
    }

    public void setUserId(long userId) {
        mPreferences.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public String getUsername() {
        return mPreferences.getString(KEY_USERNAME, null);
    }

    public void setUsername(String username) {
        mPreferences.edit().putString(KEY_USERNAME, username).apply();
    }

    public boolean isTapCategoryToBrowse() {
        return mPreferences.getBoolean(sKeyTapCategoryToBrowse, sDefaultTapCategoryToBrowse);
    }
}
