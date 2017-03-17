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

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ImageButton;

import com.fernandobarillas.linkshare.R;

import timber.log.Timber;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        setContentView(R.layout.activity_about);

        ImageButton githubButton = (ImageButton) findViewById(R.id.github_button);
        ImageButton websiteButton = (ImageButton) findViewById(R.id.website_button);
        setButtonUrlListener(githubButton, R.string.author_github_url);
        setButtonUrlListener(websiteButton, R.string.author_website_url);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setButtonUrlListener(final ImageButton button, @StringRes final int urlStringRes) {
        if (button == null) return;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String buttonUrl = getString(urlStringRes);
                openUrlExternally(buttonUrl);
            }
        });
    }
}
