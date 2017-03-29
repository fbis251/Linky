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

    private static final String ACCOUNT_PREFERENCES_NAME  = "account";
    private static final String KEY_API_URL               = "api_url";
    private static final String KEY_AUTH_STRING           = "auth_string";
    private static final String KEY_USERNAME              = "username";
    private static final String KEY_USER_ID               = "user_id";
    private static final String KEY_LAST_UPDATE_TIMESTAMP = "last_update_timestamp";

    private static String sKeyConfirmExitOnBackPress;
    private static String sKeyHideToolbarOnScroll;
    private static String sKeyTapCategoryToBrowse;
    private static String sKeyUseHttpProxy;
    private static String sKeyHttpProxyAddress;
    private static String sKeyHttpProxyPort;
    private static String sKeyUseLogcatLineNumbers;
    private static String sKeyLogErrorsOnly;
    private static String sKeyLogHttpCalls;

    private static boolean sDefaultConfirmExitOnBackPress;
    private static boolean sDefaultHideToolbarOnScroll;
    private static boolean sDefaultTapCategoryToBrowse;
    private static boolean sDefaultUseHttpProxy;
    private static String  sDefaultHttpProxyAddress;
    private static String  sDefaultHttpProxyPort;
    private static boolean sDefaultUseLogcatLineNumbers;
    private static boolean sDefaultLogErrorsOnly;
    private static boolean sDefaultLogHttpCalls;

    private SharedPreferences mPreferences;
    private SharedPreferences mAccountPreferences;

    public AppPreferences(Context context) {
        if (context == null) return;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mAccountPreferences =
                context.getSharedPreferences(ACCOUNT_PREFERENCES_NAME, Context.MODE_PRIVATE);

        Resources resources = context.getResources();

        // Get preference key strings
        sKeyConfirmExitOnBackPress =
                context.getString(R.string.preference_confirm_exit_on_back_press_key);
        sKeyHideToolbarOnScroll = context.getString(R.string.preference_hide_toolbar_on_scroll_key);
        sKeyTapCategoryToBrowse = context.getString(R.string.preference_tap_category_key);
        sKeyUseHttpProxy = context.getString(R.string.preference_use_http_proxy_key);
        sKeyHttpProxyAddress = context.getString(R.string.preference_http_proxy_address_key);
        sKeyHttpProxyPort = context.getString(R.string.preference_http_proxy_port_key);
        sKeyUseLogcatLineNumbers =
                context.getString(R.string.preference_use_logcat_line_numbers_key);
        sKeyLogErrorsOnly = context.getString(R.string.preference_log_errors_only_key);
        sKeyLogHttpCalls = context.getString(R.string.preference_log_http_calls_key);

        // Get default preference values
        if (resources != null) {
            sDefaultConfirmExitOnBackPress =
                    resources.getBoolean(R.bool.preference_confirm_exit_on_back_press_default);
            sDefaultHideToolbarOnScroll =
                    resources.getBoolean(R.bool.preference_hide_toolbar_on_scroll_default);
            sDefaultTapCategoryToBrowse =
                    resources.getBoolean(R.bool.preference_tap_category_default);
            sDefaultUseHttpProxy = resources.getBoolean(R.bool.preference_use_http_proxy_default);
            sDefaultHttpProxyAddress =
                    resources.getString(R.string.preference_http_proxy_address_default);
            sDefaultHttpProxyPort =
                    resources.getString(R.string.preference_http_proxy_port_default);
            sDefaultUseLogcatLineNumbers =
                    resources.getBoolean(R.bool.preference_use_logcat_line_numbers_default);
            sDefaultLogErrorsOnly = resources.getBoolean(R.bool.preference_log_errors_only_default);
            sDefaultLogHttpCalls = resources.getBoolean(R.bool.preference_log_http_calls_default);
        }
    }

    public void deleteAccount() {
        mAccountPreferences.edit().clear().commit();
    }

    public URL getApiUrl() {
        try {
            return new URL(mAccountPreferences.getString(KEY_API_URL, null));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public String getAuthString() {
        return mAccountPreferences.getString(KEY_AUTH_STRING, null);
    }

    public String getHttpProxyAddress() {
        return mPreferences.getString(sKeyHttpProxyAddress, sDefaultHttpProxyAddress);
    }

    public int getHttpProxyPort() throws NumberFormatException {
        String portString = mPreferences.getString(sKeyHttpProxyPort, sDefaultHttpProxyPort);
        return Integer.parseInt(portString);
    }

    public long getLastUpdateTimestamp() {
        return mAccountPreferences.getLong(KEY_LAST_UPDATE_TIMESTAMP, -1);
    }

    public long getUserId() {
        return mAccountPreferences.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return mAccountPreferences.getString(KEY_USERNAME, null);
    }

    public boolean isConfirmExitOnBackPress() {
        return mPreferences.getBoolean(sKeyConfirmExitOnBackPress, sDefaultConfirmExitOnBackPress);
    }

    public boolean isHideToolbarOnScroll() {
        return mPreferences.getBoolean(sKeyHideToolbarOnScroll, sDefaultHideToolbarOnScroll);
    }

    public boolean isLogErrorsOnly() {
        return mPreferences.getBoolean(sKeyLogErrorsOnly, sDefaultLogErrorsOnly);
    }

    public boolean isLogHttpCalls() {
        return mPreferences.getBoolean(sKeyLogHttpCalls, sDefaultLogHttpCalls);
    }

    public boolean isTapCategoryToBrowse() {
        return mPreferences.getBoolean(sKeyTapCategoryToBrowse, sDefaultTapCategoryToBrowse);
    }

    public boolean isUseHttpProxy() {
        return mPreferences.getBoolean(sKeyUseHttpProxy, sDefaultUseHttpProxy);
    }

    public boolean isUseLogcatLineNumbers() {
        return mPreferences.getBoolean(sKeyUseLogcatLineNumbers, sDefaultUseLogcatLineNumbers);
    }

    public void setApiUrl(String apiUrl) {
        mAccountPreferences.edit().putString(KEY_API_URL, apiUrl).apply();
    }

    public void setAuthString(String authString) {
        mAccountPreferences.edit().putString(KEY_AUTH_STRING, authString).apply();
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        mAccountPreferences.edit().putLong(KEY_LAST_UPDATE_TIMESTAMP, lastUpdateTimestamp).apply();
    }

    public void setUserId(long userId) {
        mAccountPreferences.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public void setUsername(String username) {
        mAccountPreferences.edit().putString(KEY_USERNAME, username).apply();
    }
}
