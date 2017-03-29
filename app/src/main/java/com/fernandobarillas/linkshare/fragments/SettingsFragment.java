package com.fernandobarillas.linkshare.fragments;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.activities.SettingsActivity;

/**
 * A Fragment that displays Preferences
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

        Preference licensesPreference = findPreference(getString(R.string.preference_licenses_key));
        if (licensesPreference != null) {
            licensesPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mActivity.openLicensesDialog();
                    return true;
                }
            });
        }

        Preference aboutPreference = findPreference(getString(R.string.preference_about_key));
        if (aboutPreference != null) {
            aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mActivity.openAbout();
                    return true;
                }
            });
        }
    }
}
