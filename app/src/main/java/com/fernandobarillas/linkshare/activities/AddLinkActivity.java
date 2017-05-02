package com.fernandobarillas.linkshare.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databinding.ActivityAddLinkBinding;
import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.utils.ResponseUtils;
import com.varunest.sparkbutton.SparkButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Created by fb on 1/29/16.
 */
public class AddLinkActivity extends BaseLinkActivity {

    private static final int SPARK_ANIMATION_DELAY = 200; // ms before spark animation plays

    private ActivityAddLinkBinding mBinding;
    private ProgressBar            mProgressBar;
    private LinearLayout           mErrorLayout;
    private LinearLayout           mSuccessLayout;
    private SparkButton            mErrorSpark;
    private SparkButton            mSuccessSpark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        serviceSetup();

        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        Link intentLink;
        if (intentReader.isShareIntent()) {
            String title = intentReader.getSubject();
            String url = intentReader.getText().toString();
            // TODO: 12/27/16 need better validation here, what happens if the URL is also null?
            if (TextUtils.isEmpty(title)) title = "";
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

    private void addLink(final Link newLink) {
        Timber.v("addLink() called with: " + "newLink = [" + newLink + "]");
        if (newLink == null) {
            // TODO: Show error
            return;
        }

        showProgress(true);
        AddLinkRequest linkRequest = new AddLinkRequest(newLink);
        Call<AddLinkResponse> call = mLinkyApi.addLink(linkRequest);
        Timber.i("addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<AddLinkResponse>() {
            @Override
            public void onFailure(Call<AddLinkResponse> call, Throwable t) {
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Timber.e(errorMessage);
                showResultLayout(false);
                finish();
            }

            @Override
            public void onResponse(Call<AddLinkResponse> call, Response<AddLinkResponse> response) {
                Timber.i("onResponse: " + ResponseUtils.httpCodeString(response));
                if (response.isSuccessful()) {
                    AddLinkResponse addLinkResponse = response.body();
                    if (addLinkResponse == null
                            || !addLinkResponse.isSuccessful()
                            || addLinkResponse.getLink() == null) {
                        Timber.e("onResponse: API returned error when adding Link: " + newLink);
                        showResultLayout(false);
                        return;
                    }
                    Link responseLink = addLinkResponse.getLink();
                    Timber.i("onResponse: Response link: " + responseLink);
                    mLinkStorage.add(responseLink);
                    Timber.i("onResponse: Added Link: " + responseLink);
                    mBinding.setAddHandler(new AddLinkHandler(responseLink));
                    showResultLayout(true);
                } else {
                    Timber.e("onResponse: " + response.body());
                    showResultLayout(false);
                }
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
        Timber.v("editLink() called with: " + "link = [" + link + "]");
        if (link == null) {
            Timber.e("editLink: Link was null, cannot edit");
            return;
        }
        EditLinkActivity.start(this, link.getLinkId());
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
        Timber.v("showResultLayout() called with: " + "isSuccessful = [" + isSuccessful + "]");
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

        private final Link mLink;

        public AddLinkHandler(Link link) {
            mLink = link;
        }

        public void onClickEdit(View view) {
            Timber.v("onClickEdit() called with: " + "view = [" + view + "]");
            editLink(mLink);
        }

        public void onClickRetry(View view) {
            Timber.v("onClickRetry() called with: " + "view = [" + view + "]");
            addLink(mLink);
        }
    }
}
