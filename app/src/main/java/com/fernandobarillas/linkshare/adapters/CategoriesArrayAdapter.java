package com.fernandobarillas.linkshare.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.databases.LinkStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * An ArrayAdapter that provides search results from Link categories
 */
public class CategoriesArrayAdapter extends ArrayAdapter<String> {
    private Filter             mFilter;
    private RealmConfiguration mRealmConfiguration;

    public CategoriesArrayAdapter(Context context, int resource, LinksApp linksApp) {
        super(context, resource);
        mRealmConfiguration = linksApp.getRealmConfiguration();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CategoriesFilter();
        }
        return mFilter;
    }

    private class CategoriesFilter extends Filter {
        private static final String LOG_TAG = "CategoriesFilter";

        private final FilterResults results = new FilterResults();

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            Log.v(LOG_TAG,
                    "performFiltering() called with: " + "charSequence = [" + charSequence + "]");
            if (charSequence == null) return results;
            String searchString = charSequence.toString();

            Realm mRealm;
            LinkStorage mLinkStorage;
            mRealm = Realm.getInstance(mRealmConfiguration);
            mLinkStorage = new LinkStorage(mRealm);
            Set<String> categoriesSet = mLinkStorage.findByCategoryString(searchString);
            if (categoriesSet.size() > 0) {
                ArrayList<String> categoriesResults = new ArrayList<>(categoriesSet);
                results.values = categoriesResults;
                results.count = categoriesResults.size();
                for (String result : categoriesResults) {
                    Log.v(LOG_TAG, "performFiltering: result = [" + result + "]");
                }
            }
            mRealm.close();
            Log.d(LOG_TAG, "performFiltering() returned: " + results);
            return results;
        }

        @Override
        protected void publishResults(
                CharSequence charSequence, FilterResults filterResults) {
            Log.v(LOG_TAG,
                    "publishResults() called with: "
                            + "charSequence = ["
                            + charSequence
                            + "], filterResults = ["
                            + filterResults
                            + "]");

            Log.v(LOG_TAG,
                    "publishResults: results.count = ["
                            + results.count
                            + "], results.values = ["
                            + results.values
                            + "]");

            if (results.count > 0) {
                clear();
                //noinspection unchecked
                addAll((List<String>) results.values);
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
