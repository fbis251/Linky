package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.util.Log;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.licenses.AndroidSupportPreferenceV7FixLicense;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;

public class SettingsActivity extends BaseActivity {

    private static final String LOG_TAG = "SettingsActivity";

    private LicensesDialog mLicensesDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG,
                "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        setContentView(R.layout.activity_settings);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.v(LOG_TAG, "onSupportNavigateUp()");
        onBackPressed();
        return true;
    }

    public void openLicensesDialog() {
        Log.v(LOG_TAG, "openLicensesDialog()");
        if (mLicensesDialog == null) {
            LicenseResolver.registerLicense(new AndroidSupportPreferenceV7FixLicense());
            mLicensesDialog = new LicensesDialog.Builder(this).setNotices(R.raw.notices)
                    .setThemeResourceId(R.style.AppDialogTheme)
                    .setIncludeOwnLicense(true)
                    .build();
        }
        mLicensesDialog.showAppCompat();
    }

}
