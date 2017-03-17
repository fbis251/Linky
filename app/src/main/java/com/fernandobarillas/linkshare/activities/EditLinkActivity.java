package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.CategoriesArrayAdapter;
import com.fernandobarillas.linkshare.databinding.ActivityEditLinkBinding;
import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class EditLinkActivity extends BaseLinkActivity {

    public static final String EXTRA_LINK_ID = "link_id";

    // Restore/Save instance state keys
    private static final String STATE_TITLE       = "title";
    private static final String STATE_URL         = "url";
    private static final String STATE_CATEGORY    = "category";
    private static final String STATE_IS_ARCHIVED = "is_archived";
    private static final String STATE_IS_FAVORITE = "is_favorite";

    private EditText             mTitleEditText;
    private EditText             mUrlEditText;
    private AutoCompleteTextView mCategoryEditText;
    private SwitchCompat         mArchivedSwitch;
    private SwitchCompat         mFavoriteSwitch;

    private Link mIntentLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceSetup();

        mIntentLink = null;
        Intent intent = getIntent();
        if (intent != null) {
            Timber.i("onCreate: Itent: %s", intent);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                if (intent.hasExtra(EXTRA_LINK_ID)) {
                    long linkId = extras.getLong(EXTRA_LINK_ID);
                    Timber.i("onCreate: Intent link ID: %d", linkId);
                    mIntentLink = mLinkStorage.findByLinkId(linkId);
                }
            }
        }

        if (mIntentLink == null) {
            Timber.e("onCreate: Intent Link was null, cannot edit Link");
            // Show UI error
            finish();
            return;
        }

        ActivityEditLinkBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_edit_link);
        binding.setLink(mIntentLink);
        binding.setEditHandler(new EditLinkHandler(mIntentLink));

        mTitleEditText = binding.editLinkTitle;
        mUrlEditText = binding.editLinkUrl;
        mCategoryEditText = binding.editLinkCategory;
        mArchivedSwitch = binding.editLinkArchivedSwitch;
        mFavoriteSwitch = binding.editLinkFavoriteSwitch;

        // Don't allow the title to contain any \n characters
        InputFilter[] noReturnsInputFilter = new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(
                            CharSequence charSequence,
                            int i,
                            int i1,
                            Spanned spanned,
                            int i2,
                            int i3) {
                        String input = charSequence.toString();
                        if (input.contains("\n")) {
                            // Delete all spaces
                            return input.replaceAll("\n", "");
                        }
                        return charSequence;
                    }
                }
        };

        mTitleEditText.setFilters(noReturnsInputFilter);
        mCategoryEditText.setFilters(noReturnsInputFilter);

        CategoriesArrayAdapter categoriesAdapter = new CategoriesArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line,
                mLinksApp);
        mCategoryEditText.setAdapter(categoriesAdapter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Timber.v("onRestoreInstanceState() called with: "
                + "savedInstanceState = ["
                + savedInstanceState
                + "]");
        super.onRestoreInstanceState(savedInstanceState);

        if (mTitleEditText != null) {
            String savedTitle = savedInstanceState.getString(STATE_TITLE);
            Timber.v("onRestoreInstanceState: savedTitle = [" + savedTitle + "]");
            mTitleEditText.addOnAttachStateChangeListener(new RestoreTextOnAttachListener(
                    mTitleEditText,
                    savedTitle));
        }

        if (mUrlEditText != null) {
            String savedUrl = savedInstanceState.getString(STATE_URL);
            Timber.v("onRestoreInstanceState: savedUrl = [" + savedUrl + "]");
            mUrlEditText.addOnAttachStateChangeListener(new RestoreTextOnAttachListener(mUrlEditText,
                    savedUrl));
        }

        if (mCategoryEditText != null) {
            String savedCategory = savedInstanceState.getString(STATE_CATEGORY);
            Timber.v("onRestoreInstanceState: savedCategory = [" + savedCategory + "]");
            mCategoryEditText.addOnAttachStateChangeListener(new RestoreTextOnAttachListener(
                    mCategoryEditText,
                    savedCategory));
        }

        if (mArchivedSwitch != null) {
            boolean savedIsArchived = savedInstanceState.getBoolean(STATE_IS_ARCHIVED);
            Timber.v("onRestoreInstanceState: savedIsArchived = [" + savedIsArchived + "]");
            mArchivedSwitch.addOnAttachStateChangeListener(new RestoreSwitchOnAttachListener(
                    mArchivedSwitch,
                    savedIsArchived));
        }

        if (mFavoriteSwitch != null) {
            boolean savedIsFavorite = savedInstanceState.getBoolean(STATE_IS_FAVORITE);
            Timber.v("onRestoreInstanceState: savedIsFavorite = [" + savedIsFavorite + "]");
            mFavoriteSwitch.addOnAttachStateChangeListener(new RestoreSwitchOnAttachListener(
                    mFavoriteSwitch,
                    savedIsFavorite));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.v("onSaveInstanceState() called with: " + "outState = [" + outState + "]");
        // TODO: Save the state of all the form inputs
        if (mIntentLink != null) {
            outState.putLong(EXTRA_LINK_ID, mIntentLink.getLinkId());
        }

        if (mTitleEditText != null) {
            outState.putString(STATE_TITLE, mTitleEditText.getText().toString());
        }

        if (mCategoryEditText != null) {
            outState.putString(STATE_CATEGORY, mCategoryEditText.getText().toString());
        }

        if (mUrlEditText != null) {
            outState.putString(STATE_URL, mUrlEditText.getText().toString());
        }

        if (mArchivedSwitch != null) {
            outState.putBoolean(STATE_IS_ARCHIVED, mArchivedSwitch.isChecked());
        }

        if (mFavoriteSwitch != null) {
            outState.putBoolean(STATE_IS_FAVORITE, mFavoriteSwitch.isChecked());
        }

        super.onSaveInstanceState(outState);
    }

    private void updateLink(final Link link) {
        Timber.v("updateLink() called with: " + "link = [" + link + "]");
        AddLinkRequest request = new AddLinkRequest(link);
        Timber.i("updateLink: request = [" + request + "]");
        mLinkService.updateLink(link.getLinkId(), request).enqueue(new Callback<AddLinkResponse>() {
            @Override
            public void onResponse(Call<AddLinkResponse> call, Response<AddLinkResponse> response) {
                Timber.v("updateLink onResponse() called with: "
                        + "call = ["
                        + call
                        + "], response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    mLinkStorage.add(link);
                    // Let the caller know that it's okay to update the adapter with the new changes
                    setResult(RESULT_OK, new Intent());
                } else {
                    // TODO: Show UI error
                    Timber.e("onResponse: Unsuccessful response");
                    ResponseBody errorResponse = response.errorBody();
                    if (errorResponse != null) {
                        try {
                            Timber.e("onResponse: errorResponse = ["
                                    + errorResponse.string()
                                    + "]");
                        } catch (IOException e) {
                            Timber.e(e, "onResponse: onResponse: Could not parse error response");
                        }
                    }
                }

                finish();
            }

            @Override
            public void onFailure(Call<AddLinkResponse> call, Throwable t) {
                Timber.v("onFailure() called with: " + "call = [" + call + "], t = [" + t + "]");
            }
        });
    }

    public class EditLinkHandler {
        private Link mOldLink;

        public EditLinkHandler(Link oldLink) {
            Timber.v("EditLinkHandler() called with: " + "oldLink = [" + oldLink + "]");
            mOldLink = oldLink;
        }

        public void onClickCancel(View view) {
            Timber.v("onClickCancel() called with: " + "view = [" + view + "]");
            finish();
        }

        public void onClickSave(View view) {
            Timber.v("onClickSave() called with: " + "view = [" + view + "]");
            if (mOldLink == null) {
                // TODO: Handle error
                return;
            }

            Link editedLink = new Link(mOldLink.getLinkId(),
                    mCategoryEditText.getText().toString(),
                    mArchivedSwitch.isChecked(),
                    mFavoriteSwitch.isChecked(),
                    mOldLink.getTimestamp(),
                    mTitleEditText.getText().toString(),
                    mUrlEditText.getText().toString());

            updateLink(editedLink);
        }
    }

    private class RestoreSwitchOnAttachListener implements View.OnAttachStateChangeListener {
        private SwitchCompat mSwitchCompat;
        private boolean      mIsChecked;

        public RestoreSwitchOnAttachListener(SwitchCompat switchCompat, boolean isChecked) {
            Timber.v("RestoreSwitchOnAttachListener() called with: "
                    + "switchCompat = ["
                    + switchCompat
                    + "], isChecked = ["
                    + isChecked
                    + "]");
            mSwitchCompat = switchCompat;
            mIsChecked = isChecked;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            Timber.v("RestoreSwitchOnAttachListener() called with: "
                    + "switchCompat = ["
                    + mSwitchCompat
                    + "], isChecked = ["
                    + mIsChecked
                    + "]");
            if (mSwitchCompat == null) return;
            mSwitchCompat.setChecked(mIsChecked);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    }

    private class RestoreTextOnAttachListener implements View.OnAttachStateChangeListener {
        private EditText mEditText;
        private String   mSavedString;

        private RestoreTextOnAttachListener(EditText editText, String savedString) {
            mEditText = editText;
            mSavedString = savedString;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            Timber.v("RestoreTextOnAttachListener() called with: "
                    + "editText = ["
                    + mEditText
                    + "], savedString = ["
                    + mSavedString
                    + "]");
            if (mEditText == null) return;
            mEditText.setText(mSavedString);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    }
}
