package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.licenses.AndroidSupportPreferenceV7FixLicense;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;
import timber.log.Timber;

public class SettingsActivity extends BaseActivity {

    private LicensesDialog mLicensesDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        setContentView(R.layout.activity_settings);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Timber.v("onSupportNavigateUp()");
        onBackPressed();
        return true;
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
        mLicensesDialog.showAppCompat();
    }

}
