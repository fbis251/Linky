package com.fernandobarillas.linkshare.licenses;

import android.content.Context;

import com.fernandobarillas.linkshare.R;

import de.psdev.licensesdialog.licenses.License;

/**
 * Created by fb on 3/8/17.
 */
public class AndroidSupportPreferenceV7FixLicense extends License {
    public AndroidSupportPreferenceV7FixLicense() {
    }

    public String getName() {
        return "The Unlicense";
    }

    public String readSummaryTextFromResources(Context context) {
        return this.getContent(context, R.raw.android_support_preference_v7_fix_license_summary);
    }

    public String readFullTextFromResources(Context context) {
        return this.getContent(context, R.raw.android_support_preference_v7_fix_license);
    }

    public String getVersion() {
        return "";
    }

    public String getUrl() {
        return "https://unlicense.org/";
    }
}
