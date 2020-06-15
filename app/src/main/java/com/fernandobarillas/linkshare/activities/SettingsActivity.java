package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.licenses.AndroidSupportPreferenceV7FixLicense;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;
import timber.log.Timber;

/**
 * An Activity that allows the user to change their preferences
 */

public class SettingsActivity extends BaseActivity {

    private LicensesDialog mLicensesDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Timber.v("onSupportNavigateUp()");
        onBackPressed();
        return true;
    }

    public void openAbout() {
        Timber.v("openAbout() called");
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void openLicensesDialog() {
        Timber.v("openLicensesDialog()");
        if (mLicensesDialog == null) {
            LicenseResolver.registerLicense(new AndroidSupportPreferenceV7FixLicense());
            mLicensesDialog = new LicensesDialog.Builder(this).setNotices(R.raw.notices)
                    .setThemeResourceId(R.style.AppDialogTheme)
                    .setIncludeOwnLicense(true)
                    .build();
        }
        mLicensesDialog.show();
    }

}
