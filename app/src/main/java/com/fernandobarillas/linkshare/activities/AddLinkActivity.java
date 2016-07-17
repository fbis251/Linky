package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databinding.ActivityAddLinkBinding;
import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;
import com.varunest.sparkbutton.SparkButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by fb on 1/29/16.
 */
public class AddLinkActivity extends BaseLinkActivity {
    private static final String LOG_TAG = "AddLinkActivity";

    private static final int SPARK_ANIMATION_DELAY = 150; // ms before spark animation plays

    private ActivityAddLinkBinding mBinding;
    private ProgressBar            mProgressBar;
    private LinearLayout           mErrorLayout;
    private LinearLayout           mSuccessLayout;
    private SparkButton            mErrorSpark;
    private SparkButton            mSuccessSpark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG,
                "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        serviceSetup();

        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        Link intentLink;
        if (intentReader.isShareIntent()) {
            String title = intentReader.getSubject();
            String url = intentReader.getText().toString();
            intentLink = new Link(url);
            intentLink.setTitle(title);
            intentLink.setCategory("");
        } else {
            finish();
            return;
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_link);
        mBinding.setLink(intentLink);
        mBinding.setAddHandler(new AddLinkHandler(intentLink));

        mProgressBar = mBinding.linkSaveProgressBar;
        mErrorLayout = mBinding.linkSaveErrorLayout;
        mSuccessLayout = mBinding.linkSaveSuccessLayout;
        mErrorSpark = mBinding.linkSaveErrorSpark;
        mSuccessSpark = mBinding.linkSaveSuccessSpark;

        addLink(intentLink);
    }

    public void addLink(final Link newLink) {
        Log.v(LOG_TAG, "addLink() called with: " + "newLink = [" + newLink + "]");
        if (newLink == null) {
            // TODO: Show error
            return;
        }

        showProgress(true);
        AddLinkRequest linkRequest = new AddLinkRequest(newLink);
        Call<AddLinkResponse> call = mLinkService.addLink(linkRequest);
        Log.i(LOG_TAG, "addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<AddLinkResponse>() {
            @Override
            public void onResponse(Call<AddLinkResponse> call, Response<AddLinkResponse> response) {
                Log.i(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (response.isSuccessful()) {
                    AddLinkResponse addLinkResponse = response.body();
                    if (addLinkResponse == null
                            || !addLinkResponse.isSuccessful()
                            || addLinkResponse.getLink() == null) {
                        Log.e(LOG_TAG,
                                "onResponse: API returned error when adding Link: " + newLink);
                        showResultLayout(false);
                        return;
                    }
                    Link responseLink = addLinkResponse.getLink();
                    Log.i(LOG_TAG, "onResponse: Response link: " + responseLink);
                    mLinkStorage.add(responseLink);
                    Log.i(LOG_TAG, "onResponse: Added Link: " + responseLink);
                    mBinding.setAddHandler(new AddLinkHandler(responseLink));
                    showResultLayout(true);
                } else {
                    Log.e(LOG_TAG, "onResponse: " + response.body());
                    showResultLayout(false);
                }
            }

            @Override
            public void onFailure(Call<AddLinkResponse> call, Throwable t) {
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showResultLayout(false);
                finish();
            }
        });
    }

    private void delayedAnimateSpark(final SparkButton sparkButton) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sparkButton.playAnimation();
            }
        }, SPARK_ANIMATION_DELAY);
    }

    private void editLink(final Link link) {
        Log.v(LOG_TAG, "editLink() called with: " + "link = [" + link + "]");
        if (link == null) {
            Log.e(LOG_TAG, "editLink: Link was null, cannot edit");
            return;
        }
        Intent editIntent = new Intent(getApplicationContext(), EditLinkActivity.class);
        editIntent.putExtra(EditLinkActivity.EXTRA_LINK_ID, link.getLinkId());
        startActivity(editIntent);
        finish();
    }

    private void showErrorLayout(boolean show) {
        if (mErrorLayout == null || mErrorSpark == null) return;
        mErrorLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) delayedAnimateSpark(mErrorSpark);
    }

    private void showProgress(boolean show) {
        if (mProgressBar == null) return;
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        showSuccessLayout(false);
        showErrorLayout(false);
    }

    private void showResultLayout(boolean isSuccessful) {
        Log.v(LOG_TAG,
                "showResultLayout() called with: " + "isSuccessful = [" + isSuccessful + "]");
        showProgress(false);
        showSuccessLayout(isSuccessful);
        showErrorLayout(!isSuccessful);
    }

    private void showSuccessLayout(boolean show) {
        if (mSuccessLayout == null || mSuccessSpark == null) return;
        mSuccessLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) delayedAnimateSpark(mSuccessSpark);
    }

    public class AddLinkHandler {

        private Link mLink;

        public AddLinkHandler(Link link) {
            mLink = link;
        }

        public void onClickEdit(View view) {
            Log.v(LOG_TAG, "onClickEdit() called with: " + "view = [" + view + "]");
            editLink(mLink);
        }

        public void onClickRetry(View view) {
            Log.v(LOG_TAG, "onClickRetry() called with: " + "view = [" + view + "]");
            addLink(mLink);
        }
    }
}
