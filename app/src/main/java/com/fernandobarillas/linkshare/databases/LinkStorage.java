package com.fernandobarillas.linkshare.databases;

import android.util.Log;

import com.fernandobarillas.linkshare.models.Link;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by fb on 2/7/16.
 */
public class LinkStorage {
    private static final String LOG_TAG = "LinkStorage";
    private Realm mRealm;

    public LinkStorage(Realm realm) {
        mRealm = realm;
    }

    public void add(final Link link) {
        Log.v(LOG_TAG, "add() called with: " + "link = [" + link + "]");
        Log.i(LOG_TAG, "add: Current count: " + getLinksCount());
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {
                Link newLink = realm.createObject(Link.class);
                newLink.copy(link);
                Log.i(LOG_TAG, "execute: Added " + newLink);
            }
        });
    }

    public List<Link> findByUrl(String url) {
        Log.v(LOG_TAG, "findByUrl() called with: " + "url = [" + url + "]");
        return new ArrayList<>(mRealm.where(Link.class).equalTo("url", url).findAll());
    }

    public List<Link> getAllLinks() {
        Log.v(LOG_TAG, "getAllLinks()");
        RealmResults<Link> results =
                mRealm.where(Link.class).findAllSorted("timestamp", Sort.ASCENDING);
        // TODO: Convert to list and return
        return new ArrayList<>(results);
    }

    public long getLinksCount() {
        return mRealm.where(Link.class).count();
    }

    public void remove(final Link link) {
        Log.v(LOG_TAG, "remove() called with: " + "link = [" + link + "]");
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {
                link.deleteFromRealm();
            }
        });
    }

    public void replaceLinks(final List<Link> newLinksList) {
        Log.v(LOG_TAG, "replaceLinks() called with: " + "newLinksList = [" + newLinksList + "]");
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {
                mRealm.delete(Link.class);
                for (Link link : newLinksList) {
                    Link newLink = mRealm.createObject(Link.class);
                    newLink.copy(link);
                }
            }
        });
    }
}
