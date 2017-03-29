package com.fernandobarillas.linkshare.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.databases.LinkStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import timber.log.Timber;

/**
 * An ArrayAdapter that provides search results from Link categories
 */
public class CategoriesArrayAdapter extends ArrayAdapter<String> {

    private Filter mFilter;

    public CategoriesArrayAdapter(final Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
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
        private final FilterResults results = new FilterResults();

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            Timber.v("performFiltering() called with: " + "charSequence = [" + charSequence + "]");
            if (charSequence == null) return results;
            String searchString = charSequence.toString();
            // Initialize a Realm instance here since this code is called from a different thread
            // than the passed-in Context and Realm objects are only valid on the thread they were
            // created on
            Realm realm = Realm.getDefaultInstance();
            LinkStorage linkStorage = new LinkStorage(realm);
            Set<String> categoriesSet = linkStorage.findByCategoryString(searchString);
            List<String> categories = new ArrayList<>(categoriesSet);
            results.values = categories;
            results.count = categories.size();
            if (BuildConfig.DEBUG) {
                for (String category : categories) {
                    Timber.v("performFiltering: category = [" + category + "]");
                }
            }
            // We're done with the realm instance, we can safely close
            realm.close();
            Timber.d("performFiltering() returned: " + results);
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            Timber.v("publishResults() called with: "
                    + "charSequence = ["
                    + charSequence
                    + "], filterResults = ["
                    + filterResults
                    + "]");

            Timber.v("publishResults: results.count = ["
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
