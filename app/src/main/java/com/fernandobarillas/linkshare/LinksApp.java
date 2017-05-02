package com.fernandobarillas.linkshare;

import android.app.Application;

import com.fernandobarillas.linkshare.api.LinkyApi;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.logging.Trees;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

/**
 * Created by fb on 6/15/16.
 */
public class LinksApp extends Application {

    private AppPreferences mPreferences;
    private LinkyApi       mLinkyApi;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.Tree tree;
        if (BuildConfig.DEBUG) {
            boolean isLogErrorsOnly = getPreferences().isLogErrorsOnly();
            boolean isUseLogcatLineNumbers = getPreferences().isUseLogcatLineNumbers();
            tree = new Trees.DebugTree(isLogErrorsOnly, isUseLogcatLineNumbers);
        } else {
            tree = new Trees.ReleaseTree();
        }
        Timber.plant(tree);
        Timber.v("onCreate()");

        realmInit();
    }

    public LinkyApi getLinkyApi() {
        Timber.v("getLinkyApi() called");
        return mLinkyApi;
    }

    public AppPreferences getPreferences() {
        if (mPreferences == null) {
            mPreferences = new AppPreferences(this);
        }
        return mPreferences;
    }

    public void setLinkyApi(LinkyApi linkyApi) {
        Timber.d("setLinkyApi() called with: linkyApi = [" + linkyApi + "]");
        mLinkyApi = linkyApi;
    }

    private void realmInit() {
        Timber.v("realmInit() called");
        Realm.init(this);
        RealmConfiguration realmConfiguration =
                new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }
}
