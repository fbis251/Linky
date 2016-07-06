package com.fernandobarillas.linkshare.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.api.LinkService;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;
import com.fernandobarillas.linkshare.models.ErrorResponse;
import com.fernandobarillas.linkshare.models.LoginRequest;
import com.fernandobarillas.linkshare.models.LoginResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseLinkActivity {

    private static final String PREFIX_HTTP  = "http://";
    private static final String PREFIX_HTTPS = "https://";

    // UI references.
    private CheckBox mHttpCheckbox;
    private EditText mServerAddressView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View     mProgressView;
    private View     mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();
        // Set up the login form.
        mHttpCheckbox = (CheckBox) findViewById(R.id.http_checkbox);
        mServerAddressView = (EditText) findViewById(R.id.api_url);
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);

        mServerAddressView.setText(PREFIX_HTTPS);
        mServerAddressView.setSelection(mServerAddressView.getText().length());

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        InputFilter[] noSpacesInputFilter = new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence charSequence, int i, int i1,
                            Spanned spanned, int i2, int i3) {
                        String input = charSequence.toString();
                        if (input.contains(" ")) {
                            // Delete all spaces
                            return input.replaceAll(" ", "");
                        }
                        return charSequence;
                    }
                }
        };

        // Don't allow spaces in server address or username
        mUsernameView.setFilters(noSpacesInputFilter);
        mServerAddressView.setFilters(noSpacesInputFilter);
        mServerAddressView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String s = editable.toString();
                String prefix = mHttpCheckbox.isChecked() ? PREFIX_HTTP : PREFIX_HTTPS;

                // Ensure that the address always starts with http:// or https://
                if (!s.startsWith(prefix)) {
                    String deletedPrefix = s.substring(0, prefix.length() - 1);
                    if (s.startsWith(deletedPrefix)) {
                        s = prefix + s.replaceAll(deletedPrefix, "");
                    } else {
                        s = prefix + s.replaceAll(prefix, "");
                    }
                    mServerAddressView.setText(s);
                    mServerAddressView.setSelection(prefix.length());
                }
            }
        });

        mHttpCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                String currentText = mServerAddressView.getText().toString();
                int currentSelection = mServerAddressView.getSelectionStart();
                if (isChecked) {
                    mServerAddressView.setText(currentText.replaceFirst(PREFIX_HTTPS, PREFIX_HTTP));
                    currentSelection -= 1;
                } else {
                    mServerAddressView.setText(currentText.replaceFirst(PREFIX_HTTP, PREFIX_HTTPS));
                    currentSelection += 1;
                }
                // Move the cursor to the end of the line
                mServerAddressView.setSelection(currentSelection);
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mServerAddressView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String apiUrlString = mServerAddressView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid API URL
        URL apiUrl = null;
        if (TextUtils.isEmpty(apiUrlString)) {
            mServerAddressView.setError(getString(R.string.error_field_required));
            focusView = mServerAddressView;
            cancel = true;
        } else {
            try {
                apiUrl = new URL(apiUrlString);
            } catch (MalformedURLException ignored) {
            }
            if (apiUrl == null || !ServiceGenerator.isApiUrlValid(apiUrl)) {
                Log.e(LOG_TAG, "attemptLogin: API URL was invalid, showing error in UI");
                mServerAddressView.setError(getString(R.string.error_invalid_server_address));
                focusView = mServerAddressView;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            doLogin(apiUrl, username, password);
        }
    }

    private void doLogin(final URL apiUrl, final String username, final String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        try {
            mLinkService = ServiceGenerator.createService(LinkService.class, apiUrl);
        } catch (InvalidApiUrlException e) {
            Log.e(LOG_TAG, "doLogin: ", e);
            showProgress(false);
            mServerAddressView.setError(getString(R.string.error_invalid_server_address));
            return;
        }

        Call<ResponseBody> loginCall = mLinkService.login(loginRequest);
        loginCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.v(LOG_TAG,
                        "doLogin onResponse() called with: " + "response = [" + response + "]");
                Log.v(LOG_TAG, "doLogin onResponse: Body: " + response.body());
                Log.v(LOG_TAG, "doLogin onResponse: Error body: " + response.errorBody());
                Gson gson = new Gson();
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse =
                                gson.fromJson(response.body().string(), LoginResponse.class);
                        String authToken = loginResponse.getAuthToken();
                        if (!TextUtils.isEmpty(authToken)) {
                            mPreferences.setApiUrl(apiUrl.toString());
                            mPreferences.setAuthToken(authToken);
                            mPreferences.setUsername(loginResponse.getUsername());
                            Log.i(LOG_TAG, "doLogin Login completed, starting LinksListActivity");
                            startActivity(
                                    new Intent(getApplicationContext(), LinksListActivity.class));
                            finish();
                            return;
                        }
                    } else if (response.errorBody() != null) {
                        ErrorResponse errorResponse =
                                gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        if (errorResponse != null) {
                            Log.w(LOG_TAG, "doLogin onResponse: API Response message: "
                                    + errorResponse.getStatusMessage());
                            handleLoginError(false, null, errorResponse.getStatusMessage());
                        }
                        return;
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(LOG_TAG, "doLogin onResponse: ", e);
                }

                handleLoginError(true, null, null);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.v(LOG_TAG, "doLogin onFailure() called with: " + "t = [" + t + "]");
                Log.e(LOG_TAG, "doLogin onFailure: ", t);
                handleLoginError(true, t, null);
            }
        });
    }

    private void handleLoginError(boolean isUserOrPasswordError, @Nullable Throwable t,
            @Nullable String errorMessage) {
        Log.v(LOG_TAG, "handleLoginError() called with: "
                + "t = ["
                + t
                + "], errorMessage = ["
                + errorMessage
                + "]");

        showProgress(false);
        String uiMessage;
        if (t instanceof UnknownHostException || t instanceof SSLPeerUnverifiedException) {
            mServerAddressView.setError(t.getLocalizedMessage());
            mServerAddressView.requestFocus();
        }

        if (isUserOrPasswordError) {
            uiMessage = errorMessage != null ? errorMessage
                    : getString(R.string.error_incorrect_username_or_password);
            mUsernameView.setError(uiMessage);
            mPasswordView.setError(uiMessage);
            mUsernameView.requestFocus();
        } else {
            uiMessage = errorMessage != null ? errorMessage
                    : getString(R.string.error_invalid_server_address);
            if (t instanceof UnknownHostException || t instanceof SSLPeerUnverifiedException) {
                if (t.getLocalizedMessage() != null) {
                    uiMessage = t.getLocalizedMessage();
                }
            }
            mServerAddressView.setError(uiMessage);
            mServerAddressView.requestFocus();
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }

    private boolean isUsernameValid(String username) {
        return username.length() >= 3;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

