package com.fernandobarillas.linkshare;

import android.app.Application;

import com.fernandobarillas.linkshare.api.LinkService;
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
    private LinkService    mLinkService;

    @Override
    public void onCreate() {
        super.onCreate();
        boolean isUseLogcatLineNumbers = getPreferences().isUseLogcatLineNumbers();
        Timber.Tree tree = BuildConfig.DEBUG ? new Trees.DebugTree(isUseLogcatLineNumbers)
                : new Trees.ReleaseTree();
        Timber.plant(tree);
        Timber.v("onCreate()");
    }

    public LinkService getLinkService() {
        Timber.v("getLinkService() called");
        return mLinkService;
    }

    public void setLinkService(LinkService linkService) {
        Timber.d("setLinkService() called with: linkService = [" + linkService + "]");
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
