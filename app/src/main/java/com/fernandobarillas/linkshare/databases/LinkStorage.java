package com.fernandobarillas.linkshare.databases;

import android.text.TextUtils;
import android.util.Log;

import com.fernandobarillas.linkshare.models.Link;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public void add(final Link link, final boolean generateNewLinkId) {
        Log.v(LOG_TAG, "add() called with: " + "link = [" + link + "]");
        Log.i(LOG_TAG, "add: Current count: " + getLinksCount());
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (generateNewLinkId) {
                    // Before the link is gotten from the API, use largest linkId + 1 for this Link
                    Link lastLink = getLastLink();
                    long newLinkId =
                            lastLink != null ? lastLink.getLinkId() + 1 : Long.MAX_VALUE - 1;
                    link.setLinkId(newLinkId);
                }
                realm.copyToRealmOrUpdate(link);
                Log.i(LOG_TAG, "add: Added " + link);
                Log.i(LOG_TAG, "add: New count: " + getLinksCount());
            }
        });
    }

    public List<Link> findByUrl(String url) {
        Log.v(LOG_TAG, "findByUrl() called with: " + "url = [" + url + "]");
        return new ArrayList<>(mRealm.where(Link.class).equalTo("url", url).findAll());
    }

    public RealmResults<Link> getAllArchived() {
        Log.v(LOG_TAG, "getAllArchived()");
        return mRealm.where(Link.class)
                .equalTo("isArchived", true)
                .findAllSorted("timestamp", Sort.DESCENDING);
    }

    public RealmResults<Link> getAllFavorites() {
        Log.v(LOG_TAG, "getAllFavorites()");
        return mRealm.where(Link.class)
                .equalTo("isFavorite", true)
                .findAllSorted("timestamp", Sort.DESCENDING);
    }

    public RealmResults<Link> getAllLinks() {
        Log.v(LOG_TAG, "getAllLinks()");
        return mRealm.where(Link.class).findAllSorted("timestamp", Sort.DESCENDING);
    }

    public Set<String> getCategories() {
        Log.v(LOG_TAG, "getCategories()");
        Set<String> categories = new LinkedHashSet<>();
        RealmResults<Link> result = mRealm.where(Link.class).distinct("category");
        for (Link link : result) {
            String category = link.getCategory();
            if (!TextUtils.isEmpty(category)) {
                categories.add(category.toLowerCase());
            }
        }

        for (String category : categories) {
            Log.i(LOG_TAG, "getCategories: Category: " + category);
        }

        return categories;
    }

    public Link getLastLink() {
        Link lastLink = mRealm.where(Link.class).findAllSorted("linkId", Sort.DESCENDING).first();
        Log.i(LOG_TAG, "getLastLink: Last link: " + lastLink);
        return lastLink;
    }

    public long getLinksCount() {
        return mRealm.where(Link.class).count();
    }

    public void remove(final Link link) {
        Log.v(LOG_TAG, "remove() called with: " + "link = [" + link + "]");
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                String title = link.getTitle();
                link.deleteFromRealm();
                Log.i(LOG_TAG, "remove: Removed: " + title);
            }
        });
    }

    public void replaceLinks(final List<Link> newLinksList) {
        Log.v(LOG_TAG, "replaceLinks() called with: " + "newLinksList = [" + newLinksList + "]");
        if (newLinksList == null) {
            Log.e(LOG_TAG, "replaceLinks: New Links list was null");
            return;
        }

        Log.i(LOG_TAG, "replaceLinks: New Link count: " + newLinksList.size());
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                // Delete all the Links current stored before we use the new server data
                mRealm.delete(Link.class);

                // Insert all the links we got from the server
                for (Link link : newLinksList) {
                    mRealm.copyToRealmOrUpdate(link);
                }
            }
        });
    }

    public void setArchived(final Link link, final boolean isArchived) {
        Log.v(LOG_TAG, "setArchived() called with: "
                + "link = ["
                + link
                + "], isArchived = ["
                + isArchived
                + "]");
        if (link == null) return; // TODO: Show UI error
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                link.setArchived(isArchived);
                Log.i(LOG_TAG, "setArchived: Edited Link: " + link);
            }
        });
    }

    public void setFavorite(final Link link, final boolean isFavorite) {
        Log.v(LOG_TAG, "setFavorite() called with: "
                + "link = ["
                + link
                + "], isFavorite = ["
                + isFavorite
                + "]");
        if (link == null) return; // TODO: Show UI error
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                link.setFavorite(isFavorite);
                Log.i(LOG_TAG, "setFavorite: Edited Link: " + link);
            }
        });
    }
}
