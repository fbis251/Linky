package com.fernandobarillas.linkshare.databases;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.fernandobarillas.linkshare.models.Link;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by fb on 2/7/16.
 */
public class LinkStorage {
    // Filtering
    public static final int FILTER_FRESH     = 0;
    public static final int FILTER_ALL       = 1;
    public static final int FILTER_FAVORITES = 2;
    public static final int FILTER_ARCHIVED  = 3;
    public static final int FILTER_CATEGORY  = 4;

    // Sorting
    public static final int SORT_TITLE_ASCENDING      = 0;
    public static final int SORT_TITLE_DESCENDING     = 1;
    public static final int SORT_TIMESTAMP_ASCENDING  = 2;
    public static final int SORT_TIMESTAMP_DESCENDING = 3;

    // Logging
    private static final String LOG_TAG = "LinkStorage";

    // Columns/Fields
    private static final String COLUMN_CATEGORY    = "category";
    private static final String COLUMN_LINK_ID     = "linkId";
    private static final String COLUMN_IS_ARCHIVED = "isArchived";
    private static final String COLUMN_IS_FAVORITE = "isFavorite";
    private static final String COLUMN_TIMESTAMP   = "timestamp";
    private static final String COLUMN_TITLE       = "title";
    private static final String COLUMN_URL         = "url";
    private Realm mRealm;

    public LinkStorage(Realm realm) {
        mRealm = realm;
    }

    public void add(final Link link) {
        Log.v(LOG_TAG, "add() called with: " + "link = [" + link + "]");
        Log.i(LOG_TAG, "add: Current count: " + getLinksCount());
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(link);
                Log.i(LOG_TAG, "add: Added " + link);
                Log.i(LOG_TAG, "add: New count: " + getLinksCount());
            }
        });
    }

    public RealmResults<Link> findByCategory(String category, @SortMode int sortMode) {
        Log.v(LOG_TAG, "findByCategory() called with: " + "category = [" + category + "]");
        return applyQuerySort(mRealm.where(Link.class), category, FILTER_CATEGORY, sortMode);
    }

    public Link findByLinkId(long linkId) {
        Log.v(LOG_TAG, "findByLinkId() called with: " + "linkId = [" + linkId + "]");
        return mRealm.where(Link.class).equalTo(COLUMN_LINK_ID, linkId).findFirst();
    }

    public RealmResults<Link> findByString(String searchTerm) {
        Log.v(LOG_TAG, "findByString() called with: " + "searchTerm = [" + searchTerm + "]");
        return mRealm.where(Link.class)
                .contains(COLUMN_CATEGORY, searchTerm, Case.INSENSITIVE)
                .or()
                .contains(COLUMN_TITLE, searchTerm, Case.INSENSITIVE)
                .or()
                .contains(COLUMN_URL, searchTerm, Case.INSENSITIVE)
                .findAllSorted(COLUMN_TITLE, Sort.ASCENDING);
    }

    public RealmResults<Link> findByUrl(String url) {
        Log.v(LOG_TAG, "findByUrl() called with: " + "url = [" + url + "]");
        return mRealm.where(Link.class).equalTo(COLUMN_URL, url).findAll();
    }

    public RealmResults<Link> getAllLinks(@FilterMode int filterMode, @SortMode int sortMode) {
        Log.v(LOG_TAG, "getAllLinks()");
        return applyQuerySort(mRealm.where(Link.class), null, filterMode, sortMode);
    }

    public Set<String> getCategories() {
        Log.v(LOG_TAG, "getCategories()");
        Set<String> categories = new TreeSet<>();
        RealmResults<Link> result = mRealm.where(Link.class).distinct(COLUMN_CATEGORY);
        Log.i(LOG_TAG, "getCategories: Category count: " + result.size());
        for (Link link : result) {
            String category = link.getCategory();
            if (!TextUtils.isEmpty(category)) {
                categories.add(category.toLowerCase());
            }
        }

        Log.i(LOG_TAG, "getCategories: Unique category count: " + categories.size());
        // TODO: It's better if this returns RealmResults<Link> since the caller can then be notified of newly added categories
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
                RealmObject.deleteFromRealm(link);
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

    public void setCategory(@NonNull final Link link, @Nullable final String category) {
        Log.v(LOG_TAG, "setCategory() called with: "
                + "link = ["
                + link
                + "], category = ["
                + category
                + "]");
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                link.setCategory(category);
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

    private RealmResults<Link> applyQuerySort(RealmQuery<Link> query, @Nullable String searchTerm,
            @FilterMode int filterMode, @SortMode int sortMode) {
        switch (filterMode) {
            case FILTER_FRESH:
                query = query.equalTo(COLUMN_IS_ARCHIVED, false);
                break;
            case FILTER_ALL:
                // Don't apply filtering in the query to return all results
                break;
            case FILTER_FAVORITES:
                query = query.equalTo(COLUMN_IS_FAVORITE, true);
                break;
            case FILTER_ARCHIVED:
                query = query.equalTo(COLUMN_IS_ARCHIVED, true);
                break;
            case FILTER_CATEGORY:
                query = query.equalTo(COLUMN_CATEGORY, searchTerm, Case.INSENSITIVE);
                break;
        }

        switch (sortMode) {
            case SORT_TITLE_ASCENDING:
                return query.findAllSorted(COLUMN_TITLE, Sort.ASCENDING);
            case SORT_TITLE_DESCENDING:
                return query.findAllSorted(COLUMN_TITLE, Sort.DESCENDING);
            case SORT_TIMESTAMP_ASCENDING:
                return query.findAllSorted(COLUMN_TIMESTAMP, Sort.ASCENDING);
            case SORT_TIMESTAMP_DESCENDING:
                return query.findAllSorted(COLUMN_TIMESTAMP, Sort.DESCENDING);
        }

        return null;
    }

    // Query results filtering
    @IntDef({FILTER_FRESH, FILTER_ALL, FILTER_FAVORITES, FILTER_ARCHIVED, FILTER_CATEGORY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FilterMode {
    }

    // Query results sorting
    @IntDef({
            SORT_TITLE_ASCENDING, SORT_TITLE_DESCENDING, SORT_TIMESTAMP_ASCENDING,
            SORT_TIMESTAMP_DESCENDING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SortMode {
    }
}
