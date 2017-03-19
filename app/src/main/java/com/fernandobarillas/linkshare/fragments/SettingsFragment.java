package com.fernandobarillas.linkshare.fragments;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.activities.SettingsActivity;

/**
 * Created by fb on 3/8/17.
 */

public class SettingsFragment extends PreferenceFragmentCompat {
    private SettingsActivity mActivity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.debug_preferences);
        }
        mActivity = (SettingsActivity) getActivity();
        Preference licensesPreferences =
                findPreference(getString(R.string.preference_licenses_key));
        if (licensesPreferences != null) {
            licensesPreferences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mActivity.openLicensesDialog();
                    return true;
                }
            });
        }
    }
}
