package com.fernandobarillas.linkshare.databases;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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
import timber.log.Timber;

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
    public static final int FILTER_SEARCH    = 5;

    // Sorting
    public static final int SORT_TITLE_ASCENDING      = 0;
    public static final int SORT_TITLE_DESCENDING     = 1;
    public static final int SORT_TIMESTAMP_ASCENDING  = 2;
    public static final int SORT_TIMESTAMP_DESCENDING = 3;

    // Columns/Fields
    private static final String COLUMN_CATEGORY    = "category";
    private static final String COLUMN_LINK_ID     = "linkId";
    private static final String COLUMN_IS_ARCHIVED = "isArchived";
    private static final String COLUMN_IS_FAVORITE = "isFavorite";
    private static final String COLUMN_TIMESTAMP   = "timestamp";
    private static final String COLUMN_TITLE       = "title";
    private static final String COLUMN_URL         = "url";

    private final Realm mRealm;

    public LinkStorage(Realm realm) {
        mRealm = realm;
    }

    public void add(final Link link) {
        Timber.v("add() called with: " + "link = [" + link + "]");
        Timber.i("add: Current count: " + getLinksCount());
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(link);
                Timber.i("add: Added " + link);
                Timber.i("add: New count: " + getLinksCount());
            }
        });
    }

    public void deleteAllLinks() {
        Timber.v("deleteAllLinks()");
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                mRealm.deleteAll();
                Timber.i("deleteAllLinks execute: Deleted all links from the database");
            }
        });
    }

    public RealmResults<Link> findByCategory(String category, @SortMode int sortMode) {
        Timber.v("findByCategory() called with: " + "category = [" + category + "]");
        return applyQuerySort(mRealm.where(Link.class), category, FILTER_CATEGORY, sortMode);
    }

    public Set<String> findByCategoryString(String searchTerm) {
        Timber.v("findByCategoryString() called with: " + "searchTerm = [" + searchTerm + "]");
        Set<String> categories = new TreeSet<>();
        RealmResults<Link> result = mRealm.where(Link.class)
                .contains(COLUMN_CATEGORY, searchTerm, Case.INSENSITIVE)
                .distinct(COLUMN_CATEGORY);
        Timber.i("getCategories: Category count: " + result.size());
        for (Link link : result) {
            String category = link.getCategory();
            if (!TextUtils.isEmpty(category)) {
                categories.add(category.toLowerCase());
            }
        }

        Timber.i("getCategories: Unique category count: " + categories.size());
        return categories;
    }

    public Link findByLinkId(long linkId) {
        Timber.v("findByLinkId() called with: " + "linkId = [" + linkId + "]");
        return mRealm.where(Link.class).equalTo(COLUMN_LINK_ID, linkId).findFirst();
    }

    public RealmResults<Link> findByString(String searchTerm, @SortMode int sortMode) {
        Timber.v("findByString() called with: " + "searchTerm = [" + searchTerm + "]");
        RealmQuery<Link> query = mRealm.where(Link.class);
        return applyQuerySort(query, searchTerm, FILTER_SEARCH, sortMode);
    }

    public RealmResults<Link> getAllLinks(@FilterMode int filterMode, @SortMode int sortMode) {
        Timber.v("getAllLinks()");
        return applyQuerySort(mRealm.where(Link.class), null, filterMode, sortMode);
    }

    public Set<String> getCategories() {
        Timber.v("getCategories()");
        Set<String> categories = new TreeSet<>();
        RealmResults<Link> result = mRealm.where(Link.class).distinct(COLUMN_CATEGORY);
        Timber.i("getCategories: Category count: " + result.size());
        for (Link link : result) {
            String category = link.getCategory();
            if (!TextUtils.isEmpty(category)) {
                categories.add(category.toLowerCase());
            }
        }

        Timber.i("getCategories: Unique category count: " + categories.size());
        return categories;
    }

    public long getFreshLinkCount() {
        Timber.v("getFreshLinkCount() called");
        return mRealm.where(Link.class).equalTo(COLUMN_IS_ARCHIVED, false).count();
    }

    public long getLinksCount() {
        return mRealm.where(Link.class).count();
    }

    public void remove(final Link link) {
        Timber.v("remove() called with: " + "link = [" + link + "]");
        if (link == null) return;
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                String title = link.getTitle();
                RealmObject.deleteFromRealm(link);
                Timber.i("remove: Removed: " + title);
            }
        });
    }

    public void replaceLinks(final List<Link> newLinksList) {
        Timber.v("replaceLinks() called with: " + "newLinksList = [" + newLinksList + "]");
        if (newLinksList == null) {
            Timber.e("replaceLinks: New Links list was null");
            return;
        }

        Timber.i("replaceLinks: New Link count: " + newLinksList.size());
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
        Timber.v("setArchived() called with: "
                + "link = ["
                + link
                + "], isArchived = ["
                + isArchived
                + "]");
        if (link == null) return;
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                link.setArchived(isArchived);
                Timber.i("setArchived: Edited Link: " + link);
            }
        });
    }

    public void setFavorite(final Link link, final boolean isFavorite) {
        Timber.v("setFavorite() called with: "
                + "link = ["
                + link
                + "], isFavorite = ["
                + isFavorite
                + "]");
        if (link == null) return;
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                link.setFavorite(isFavorite);
                Timber.i("setFavorite: Edited Link: " + link);
            }
        });
    }

    private RealmResults<Link> applyQuerySort(
            RealmQuery<Link> query,
            @Nullable String searchTerm,
            @FilterMode int filterMode,
            @SortMode int sortMode) {
        switch (filterMode) {
            case FILTER_ALL:
                // Don't apply filtering in the query to return all results
                break;
            case FILTER_FRESH:
                query = query.equalTo(COLUMN_IS_ARCHIVED, false);
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
            case FILTER_SEARCH:
                query = query.contains(COLUMN_CATEGORY, searchTerm, Case.INSENSITIVE)
                        .or()
                        .contains(COLUMN_TITLE, searchTerm, Case.INSENSITIVE)
                        .or()
                        .contains(COLUMN_URL, searchTerm, Case.INSENSITIVE);
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
    @IntDef({
            FILTER_FRESH,
            FILTER_ALL,
            FILTER_FAVORITES,
            FILTER_ARCHIVED,
            FILTER_CATEGORY,
            FILTER_SEARCH
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FilterMode {
    }

    // Query results sorting
    @IntDef({
            SORT_TITLE_ASCENDING,
            SORT_TITLE_DESCENDING,
            SORT_TIMESTAMP_ASCENDING,
            SORT_TIMESTAMP_DESCENDING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SortMode {
    }
}
