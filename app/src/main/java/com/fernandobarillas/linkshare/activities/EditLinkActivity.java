package com.fernandobarillas.linkshare.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databinding.ActivityEditLinkBinding;
import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;

import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditLinkActivity extends BaseLinkActivity {

    public static final String EXTRA_LINK_ID = "link_id";

    private EditText mTitleEditText;
    private EditText mUrlEditText;
    private Spinner  mCategoriesSpinner;
    private Switch   mArchivedSwitch;
    private Switch   mFavoriteSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceSetup();

        Link intentLink = null;
        Intent intent = getIntent();
        if (savedInstanceState == null && intent != null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && intent.hasExtra(EXTRA_LINK_ID)) {
                long linkId = intent.getExtras().getLong(EXTRA_LINK_ID);
                intentLink = mLinkStorage.findByLinkId(linkId);
            }
        }

        if (intentLink == null) {
            // Show UI error
            finish();
            return;
        }

        ActivityEditLinkBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_edit_link);
        binding.setLink(intentLink);
        binding.setEditHandler(new EditLinkHandler(intentLink));

        mTitleEditText = binding.editLinkTitle;
        mUrlEditText = binding.editLinkUrl;
        mCategoriesSpinner = binding.editLinkCategoriesSpinner;
        mArchivedSwitch = binding.editLinkArchivedSwitch;
        mFavoriteSwitch = binding.editLinkFavoriteSwitch;

        // Don't allow the title to contain any \n characters
        InputFilter[] noReturnsInputFilter = new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence charSequence, int i, int i1,
                            Spanned spanned, int i2, int i3) {
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

        ArrayAdapter<String> categoriesAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCategoriesSpinner.setAdapter(categoriesAdapter);
        categoriesAdapter.add(getString(R.string.category_uncategorized));

        Set<String> categories = mLinkStorage.getCategories();
        for (String category : categories) {
            categoriesAdapter.add(category);
        }
        categoriesAdapter.notifyDataSetChanged();

        String currentCategory = intentLink.getCategory();
        if (!TextUtils.isEmpty(currentCategory)) {
            mCategoriesSpinner.setSelection(
                    categoriesAdapter.getPosition(currentCategory.toLowerCase()));
        }
    }

    private void updateLink(final Link link) {
        Log.v(LOG_TAG, "updateLink() called with: " + "link = [" + link + "]");
        AddLinkRequest request = new AddLinkRequest(link);
        Log.i(LOG_TAG, "updateLink: Request: " + request);
        mLinkService.updateLink(link.getLinkId(), request).enqueue(new Callback<AddLinkResponse>() {
            @Override
            public void onResponse(Call<AddLinkResponse> call, Response<AddLinkResponse> response) {
                Log.v(LOG_TAG, "updateLink onResponse() called with: "
                        + "call = ["
                        + call
                        + "], response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    mLinkStorage.add(link);
                } else {
                    // TODO: Show UI error
                    Log.e(LOG_TAG, "onResponse: Unsuccessful response");
                }
                finish();
            }

            @Override
            public void onFailure(Call<AddLinkResponse> call, Throwable t) {
                Log.v(LOG_TAG, "updateLink onFailure() called with: "
                        + "call = ["
                        + call
                        + "], t = ["
                        + t
                        + "]");
            }
        });
    }

    public class EditLinkHandler {
        private final String LOG_TAG = getClass().getSimpleName();

        private Link mOldLink;

        public EditLinkHandler(Link oldLink) {
            mOldLink = oldLink;
        }

        public void onClickSave(View view) {
            Log.v(LOG_TAG, "onClickSave() called with: " + "view = [" + view + "]");
            if (mOldLink == null) {
                // TODO: Handle error
                return;
            }

            String spinnerCategory = mCategoriesSpinner.getSelectedItem().toString();
            String category =
                    spinnerCategory.equalsIgnoreCase(getString(R.string.category_uncategorized))
                            ? "" : spinnerCategory;
            Link editedLink = new Link(mOldLink.getLinkId(), category, mArchivedSwitch.isChecked(),
                    mFavoriteSwitch.isChecked(), mOldLink.getTimestamp(),
                    mTitleEditText.getText().toString(), mUrlEditText.getText().toString());

            updateLink(editedLink);
        }
    }
}
