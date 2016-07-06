package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

public class BaseLinkActivity extends AppCompatActivity {

    protected final String LOG_TAG = getClass().getSimpleName();

    LinksApp       mLinksApp;
    LinkStorage    mLinkStorage;
    LinkService    mLinkService;
    AppPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG,
                "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Initialize the database
        mLinksApp = (LinksApp) getApplicationContext();
        mLinkStorage = mLinksApp.getLinkStorage();
        mPreferences = mLinksApp.getPreferences();
    }

    @Override
    protected void onStop() {
        Log.v(LOG_TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");
        super.onDestroy();
    }

    protected void launchLoginActivity() {
        Log.v(LOG_TAG, "launchLoginActivity()");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    protected void serviceSetup() {
        Log.v(LOG_TAG, "serviceSetup()");
        String refreshToken = mPreferences.getAuthToken();
        if (TextUtils.isEmpty(refreshToken)) {
            Log.i(LOG_TAG, "serviceSetup: No refresh token stored, starting LoginActivity");
            launchLoginActivity();
        }
        try {
            mLinkService = mLinksApp.getLinkService();
        } catch (InvalidApiUrlException e) {
            Log.e(LOG_TAG, "serviceSetup: Invalid API URL, launching login activity", e);
            launchLoginActivity();
        }
    }

    protected void setToolbarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setTitle(title);
    }
}
