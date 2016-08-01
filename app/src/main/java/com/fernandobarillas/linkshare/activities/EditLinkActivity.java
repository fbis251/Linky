package com.fernandobarillas.linkshare.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databinding.ActivityEditLinkBinding;
import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;

import java.io.IOException;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditLinkActivity extends BaseLinkActivity
        implements AdapterView.OnItemSelectedListener {

    public static final String EXTRA_LINK_ID = "link_id";

    private static final int ADD_NEW_CATEGORY_POSITION = 1;

    private EditText mTitleEditText;
    private EditText mUrlEditText;
    private Spinner  mCategoriesSpinner;
    private Switch   mArchivedSwitch;
    private Switch   mFavoriteSwitch;

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

        populateCategoriesSpinner(null);
        mCategoriesSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (position == ADD_NEW_CATEGORY_POSITION) {
            new CategoryDialog(EditLinkActivity.this).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Leave category as is

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

    private void populateCategoriesSpinner(@Nullable final String newCategory) {
        Log.v(LOG_TAG, "populateCategoriesSpinner() called with: "
                + "newCategory = ["
                + newCategory
                + "]");
        ArrayAdapter<String> categoriesAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCategoriesSpinner.setAdapter(categoriesAdapter);
        categoriesAdapter.add(getString(R.string.category_uncategorized));
        // The add new category position has to match the value of: ADD_NEW_CATEGORY_POSITION
        categoriesAdapter.add(getString(R.string.category_add_new));

        Set<String> categories = mLinkStorage.getCategories();
        if (!TextUtils.isEmpty(newCategory)) {
            categories.add(newCategory);
        }
        for (String category : categories) {
            categoriesAdapter.add(category);
        }
        categoriesAdapter.notifyDataSetChanged();

        String currentCategory = mIntentLink.getCategory();
        if (!TextUtils.isEmpty(currentCategory)) {
            mCategoriesSpinner.setSelection(
                    categoriesAdapter.getPosition(currentCategory.toLowerCase()));
        }
        if (!TextUtils.isEmpty(newCategory)) {
            mCategoriesSpinner.setSelection(
                    categoriesAdapter.getPosition(newCategory.toLowerCase()));
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
                Log.v(LOG_TAG, "updateLink onFailure() called with: "
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

            String category = mOldLink.getCategory();
            if (mCategoriesSpinner.getSelectedItem() != null) {
                String spinnerCategory = mCategoriesSpinner.getSelectedItem().toString();
                if (!spinnerCategory.equalsIgnoreCase(getString(R.string.category_add_new))
                        && !spinnerCategory.equalsIgnoreCase(
                        getString(R.string.category_uncategorized))) {
                    category = spinnerCategory;
                }
            }

            if (category == null) category = "";
            Link editedLink = new Link(mOldLink.getLinkId(), category, mArchivedSwitch.isChecked(),
                    mFavoriteSwitch.isChecked(), mOldLink.getTimestamp(),
                    mTitleEditText.getText().toString(), mUrlEditText.getText().toString());

            updateLink(editedLink);
        }
    }

    private class CategoryDialog extends Dialog implements DialogInterface.OnCancelListener {
        private EditText mNewCategoryEditText;

        public CategoryDialog(Context context) {
            super(context);
            dialogSetup();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            Log.v(LOG_TAG,
                    "onCancel() called with: " + "dialogInterface = [" + dialogInterface + "]");
            handleCancel();
        }

        private void dialogSetup() {
            setTitle(R.string.add_new_category_title);
            setContentView(R.layout.add_new_category_dialog);
            mNewCategoryEditText = (EditText) findViewById(R.id.add_new_category_name);
            Button saveButton = (Button) findViewById(R.id.add_new_category_save_button);
            Button cancelButton = (Button) findViewById(R.id.add_new_category_cancel_button);

            if (saveButton != null) saveButton.setOnClickListener(handleSaveButton());
            if (cancelButton != null) cancelButton.setOnClickListener(handleCancelButton());
            setOnCancelListener(CategoryDialog.this);
        }

        @Nullable
        private String getNewCategory() {
            if (mNewCategoryEditText == null) return null;
            return mNewCategoryEditText.getText().toString().trim().toLowerCase();
        }

        private void handleCancel() {
            Log.v(LOG_TAG, "handleCancel()");
            populateCategoriesSpinner(null);
            dismiss();
        }

        private View.OnClickListener handleCancelButton() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(LOG_TAG,
                            "handleCancelButton onClick() called with: " + "view = [" + view + "]");
                    handleCancel();
                }
            };
        }

        private View.OnClickListener handleSaveButton() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(LOG_TAG,
                            "handleSaveButton onClick() called with: " + "view = [" + view + "]");
                    String category = getNewCategory();
                    Log.i(LOG_TAG, "handleSaveButton onClick: New category: " + category);
                    if (TextUtils.isEmpty(category)) {
                        if (mNewCategoryEditText != null) {
                            mNewCategoryEditText.setError(
                                    getString(R.string.add_new_category_empty_error));
                        }
                        return;
                    }
                    populateCategoriesSpinner(category);
                    dismiss();
                }
            };
        }
    }
}
