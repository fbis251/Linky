package com.fernandobarillas.linkshare;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by fb on 6/15/16.
 */
public class LinksApp extends Application {

    private final String LOG_TAG = getClass().getSimpleName();

    private AppPreferences mPreferences;
    private Realm          mRealm;
    private LinkStorage    mLinkStorage;
    private LinkService    mLinkService;

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "onCreate()");
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(LOG_TAG,
                "onConfigurationChanged() called with: " + "newConfig = [" + newConfig + "]");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        Log.v(LOG_TAG, "onLowMemory()");
        super.onLowMemory();
    }

    public LinkService getLinkService() throws InvalidApiUrlException {
        if (mLinkService == null) {
            getPreferences();
            mLinkService =
                    ServiceGenerator.createService(LinkService.class, mPreferences.getApiUrl(),
                            mPreferences.getAuthToken());
        }
        return mLinkService;
    }

    public LinkStorage getLinkStorage() {
        Log.v(LOG_TAG, "getLinkStorage()");
        if (mLinkStorage == null) {
            databaseSetup();
        }
        return mLinkStorage;
    }

    public AppPreferences getPreferences() {
        if (mPreferences == null) {
            mPreferences = new AppPreferences(this);
        }
        return mPreferences;
    }

    public Realm getRealm() {
        Log.v(LOG_TAG, "getRealm()");
        if (mRealm == null) {
            databaseSetup();
        }

        return mRealm;
    }

    private void databaseSetup() {
        Log.v(LOG_TAG, "databaseSetup()");
        RealmConfiguration realmConfig =
                // FIXME: Add actual database migration
                new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
        mRealm = Realm.getInstance(realmConfig);
        mLinkStorage = new LinkStorage(mRealm);
    }
}
