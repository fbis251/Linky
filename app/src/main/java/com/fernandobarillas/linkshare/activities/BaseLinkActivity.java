package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

import io.realm.Realm;

public abstract class BaseLinkActivity extends BaseActivity {

    protected final String LOG_TAG = getClass().getSimpleName();

    protected Realm       mRealm;
    protected LinkStorage mLinkStorage;
    protected LinkService mLinkService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG,
                "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Initialize the database
        mLinksApp = (LinksApp) getApplicationContext();
        mRealm = Realm.getInstance(mLinksApp.getRealmConfiguration());
        mLinkStorage = new LinkStorage(mRealm);
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");
        if (mRealm != null) {
            mRealm.close();
            mRealm = null;
        }
        if (mLinkStorage != null) mLinkStorage = null;
        super.onDestroy();
    }

    protected void performLogout() {
        Log.v(LOG_TAG, "performLogout()");
        mPreferences.deleteAllPreferences();
        mLinkStorage.deleteAllLinks();
        launchLoginActivity();
    }

    protected void serviceSetup() {
        Log.v(LOG_TAG, "serviceSetup()");
        String authToken = mPreferences.getAuthString();
        if (TextUtils.isEmpty(authToken)) {
            Log.i(LOG_TAG, "serviceSetup: No refresh token stored, starting LoginActivity");
            performLogout();
            return;
        }
        try {
            mLinkService = mLinksApp.getLinkService();
        } catch (InvalidApiUrlException e) {
            Log.e(LOG_TAG, "serviceSetup: Invalid API URL, launching login activity", e);
            performLogout();
        }
    }
}
