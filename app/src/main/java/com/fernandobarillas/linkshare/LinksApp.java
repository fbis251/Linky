package com.fernandobarillas.linkshare;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.configuration.AppPreferences;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by fb on 6/15/16.
 */
public class LinksApp extends Application {

    private final String LOG_TAG = getClass().getSimpleName();

    private AppPreferences mPreferences;
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

    public LinkService getLinkService() {
        Log.v(LOG_TAG, "getLinkService() called");
        return mLinkService;
    }

    public void setLinkService(LinkService linkService) {
        Log.d(LOG_TAG, "setLinkService() called with: linkService = [" + linkService + "]");
        mLinkService = linkService;
    }

    public AppPreferences getPreferences() {
        if (mPreferences == null) {
            mPreferences = new AppPreferences(this);
        }
        return mPreferences;
    }

    public RealmConfiguration getRealmConfiguration() {
        Realm.init(this);
        return new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build();
    }
}
