package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.text.TextUtils;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.api.LinksApi;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

import io.realm.Realm;
import timber.log.Timber;

public abstract class BaseLinkActivity extends BaseActivity {

    protected Realm       mRealm;
    protected LinkStorage mLinkStorage;
    protected LinkService mLinkService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Initialize the database
        mLinksApp = (LinksApp) getApplicationContext();
        mRealm = Realm.getInstance(mLinksApp.getRealmConfiguration());
        mLinkStorage = new LinkStorage(mRealm);
    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy()");
        if (mRealm != null) {
            mRealm.close();
            mRealm = null;
        }
        if (mLinkStorage != null) mLinkStorage = null;
        super.onDestroy();
    }

    protected void performLogout() {
        Timber.v("performLogout()");
        mPreferences.deleteAllPreferences();
        mLinkStorage.deleteAllLinks();
        launchLoginActivity();
    }

    protected void serviceSetup() {
        Timber.v("serviceSetup()");
        String authToken = mPreferences.getAuthString();
        if (TextUtils.isEmpty(authToken)) {
            Timber.i("serviceSetup: No refresh token stored, starting LoginActivity");
            performLogout();
            return;
        }
        mLinkService = mLinksApp.getLinkService();
        if (mLinkService == null) {
            try {
                LinksApi linksApi = new LinksApi(mPreferences.getApiUrl(),
                        mPreferences.getUserId(),
                        mPreferences.getAuthString());
                mLinksApp.setLinkService(linksApi.getLinkService(mPreferences));
                mLinkService = mLinksApp.getLinkService();
            } catch (InvalidApiUrlException e) {
                Timber.e("serviceSetup: Invalid API URL, launching login activity", e);
                performLogout();
            }
        }
    }
}
