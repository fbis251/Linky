/*
 * Copyright 2016 Fernando Barillas (FBis251)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fernandobarillas.linkshare.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.fernandobarillas.linkshare.LinksApp;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ShareHandler;

import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity {

    protected LinksApp       mLinksApp;
    protected AppPreferences mPreferences;
    private   Snackbar       mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Initialize the database
        mLinksApp = (LinksApp) getApplicationContext();
        mPreferences = mLinksApp.getPreferences();
    }

    @Override
    protected void onStop() {
        Timber.v("onStop()");
        super.onStop();
    }

    public void closeSoftKeyboard() {
        Timber.v("closeSoftKeyboard()");
        closeSoftKeyboard(getCurrentFocus());
    }

    public void closeSoftKeyboard(final View view) {
        Timber.v("closeSoftKeyboard() called with: " + "view = [" + view + "]");
        if (view == null) {
            Timber.d("closeSoftKeyboard: View was null, not closing keyboard");
            return;
        }
        Timber.d("closeSoftKeyboard: Closing soft keyboard");
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void dismissSnackbar() {
        Timber.v("dismissSnackbar()");
        if (mSnackbar == null) return;
        mSnackbar.dismiss();
    }

    public void openUrlExternally(final String url) {
        Timber.v("openUrlExternally() called with: " + "url = [" + url + "]");
        if (url == null) return;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make sure that we have applications installed that can handle this intent
        if (browserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(browserIntent);
        } else {
            showSnackError(getString(R.string.open_url_error), true);
        }
    }

    public void shareUrl(final String title, final String url) {
        Timber.v("shareUrl() called with: " + "title = [" + title + "], url = [" + url + "]");
        if (url == null || !ShareHandler.share(title, url, this)) {
            showSnackError(getString(R.string.share_intent_error), true);
        }
    }

    public void showSnackError(final String message, final boolean showDismissAction) {
        Snacks.Action dismissAction = null;
        if (showDismissAction) {
            dismissAction = new Snacks.Action(R.string.dismiss, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Perform no action so the SnackBar gets dismissed on click
                }
            });
        }
        showSnackError(message, dismissAction);
    }

    public void showSnackError(final String message, final Snacks.Action snackAction) {
        View view = findViewById(android.R.id.content);
        if (view == null) return;
        mSnackbar = Snacks.showError(view, message, snackAction);
    }

    public void showSnackSuccess(final String message) {
        View view = findViewById(android.R.id.content);
        if (view == null) return;
        mSnackbar = Snacks.showMessage(view, message);
    }

    protected void launchLinksListActivity() {
        Timber.v("launchLinksListActivity()");
        Intent listIntent = new Intent(this, LinksListActivity.class);
        listIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(listIntent);
        finish();
    }

    protected void launchLoginActivity() {
        Timber.v("launchLoginActivity()");
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    protected void setToolbarTitle(String title, String subTitle) {
        Timber.v("setToolbarTitle() called with: "
                + "title = ["
                + title
                + "], subTitle = ["
                + subTitle
                + "]");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setTitle(title);
        actionBar.setSubtitle(subTitle);
    }
}
