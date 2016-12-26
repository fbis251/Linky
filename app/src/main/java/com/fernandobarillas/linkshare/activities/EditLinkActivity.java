package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
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

public class EditLinkActivity extends BaseLinkActivity {

    public static final String EXTRA_LINK_ID = "link_id";

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
        if (savedInstanceState != null) {
            mIntentLink = mLinkStorage.findByLinkId(savedInstanceState.getLong(EXTRA_LINK_ID));
        }

        Intent intent = getIntent();
        if (intent != null) {
            Log.i(LOG_TAG, "onCreate: Intent: " + intent);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                if (intent.hasExtra(EXTRA_LINK_ID)) {
                    long linkId = extras.getLong(EXTRA_LINK_ID);
                    Log.i(LOG_TAG, "onCreate: Intent link ID: " + linkId);
                    mIntentLink = mLinkStorage.findByLinkId(linkId);
                }
            }
        }

        if (mIntentLink == null) {
            Log.e(LOG_TAG, "onCreate: Intent Link was null, cannot edit Link");
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
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(LOG_TAG, "onSaveInstanceState() called with: " + "outState = [" + outState + "]");
        // TODO: Save the state of all the form inputs
        if (mIntentLink != null) {
            outState.putLong(EXTRA_LINK_ID, mIntentLink.getLinkId());
        }
        super.onSaveInstanceState(outState);
    }

    private void updateLink(final Link link) {
        Log.v(LOG_TAG, "updateLink() called with: " + "link = [" + link + "]");
        AddLinkRequest request = new AddLinkRequest(link);
        Log.i(LOG_TAG, "updateLink: Request: " + request);
        mLinkService.updateLink(link.getLinkId(), request).enqueue(new Callback<AddLinkResponse>() {
            @Override
            public void onResponse(Call<AddLinkResponse> call, Response<AddLinkResponse> response) {
                Log.v(LOG_TAG,
                        "updateLink onResponse() called with: "
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
                    Log.e(LOG_TAG, "onResponse: Unsuccessful response");
                    ResponseBody errorResponse = response.errorBody();
                    if (errorResponse != null) {
                        try {
                            Log.e(LOG_TAG, "onResponse: " + errorResponse.string());
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "onResponse: Could not parse error response", e);
                        }
                    }
                }

                finish();
            }

            @Override
            public void onFailure(Call<AddLinkResponse> call, Throwable t) {
                Log.v(LOG_TAG,
                        "updateLink onFailure() called with: "
                                + "call = ["
                                + call
                                + "], t = ["
                                + t
                                + "]");
            }
        });
    }

    private void validateForm() {
        Log.v(LOG_TAG, "validateForm()");
        // TODO: implement me
    }

    public class EditLinkHandler {
        private final String LOG_TAG = getClass().getSimpleName();

        private Link mOldLink;

        public EditLinkHandler(Link oldLink) {
            Log.v(LOG_TAG, "EditLinkHandler() called with: " + "oldLink = [" + oldLink + "]");
            mOldLink = oldLink;
        }

        public void onClickCancel(View view) {
            Log.v(LOG_TAG, "onClickCancel() called with: " + "view = [" + view + "]");
            finish();
        }

        public void onClickSave(View view) {
            Log.v(LOG_TAG, "onClickSave() called with: " + "view = [" + view + "]");
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
}
