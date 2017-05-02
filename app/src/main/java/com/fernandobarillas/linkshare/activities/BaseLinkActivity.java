package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.text.TextUtils;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.api.LinkyApi;
import com.fernandobarillas.linkshare.api.LinkyService;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

import io.realm.Realm;
import timber.log.Timber;

public abstract class BaseLinkActivity extends BaseActivity {

    LinkStorage  mLinkStorage;
    LinkyApi     mLinkyApi;
    LinkyService mLinkyService;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Initialize the database
        mLinksApp = (LinksApp) getApplicationContext();
        mRealm = Realm.getDefaultInstance();
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

    void performLogout() {
        Timber.v("performLogout()");
        mPreferences.deleteAccount();
        mLinkStorage.deleteAllLinks();

        // Delete Realm files from disk
        if (mRealm != null) mRealm.close();
        deleteRecursive(getFilesDir());

        // Terminate and relaunch the application
        restartApplication();
    }

    void serviceSetup() {
        Timber.v("serviceSetup()");
        String authToken = mPreferences.getAuthString();
        if (TextUtils.isEmpty(authToken)) {
            Timber.i("serviceSetup: No refresh token stored, starting LoginActivity");
            launchLoginActivity();
            return;
        }
        mLinkyApi = mLinksApp.getLinkyApi();
        if (mLinkyApi == null) {
            try {
                mLinkyService = new LinkyService(mPreferences.getApiUrl(),
                        mPreferences.getUserId(),
                        mPreferences.getAuthString());
                mLinksApp.setLinkyApi(mLinkyService.getLinkService(mPreferences));
                mLinkyApi = mLinksApp.getLinkyApi();
            } catch (InvalidApiUrlException e) {
                Timber.e("serviceSetup: Invalid API URL, launching login activity", e);
                performLogout();
            }
        }
    }
}
