package com.fernandobarillas.linkshare.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.fernandobarillas.linkshare.api.LinksApi;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;
import com.fernandobarillas.linkshare.models.ErrorResponse;
import com.fernandobarillas.linkshare.models.LoginRequest;
import com.fernandobarillas.linkshare.models.LoginResponse;
import com.fernandobarillas.linkshare.utils.ResponseUtils;
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
import timber.log.Timber;

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
                if (id == EditorInfo.IME_ACTION_DONE
                        || id == R.id.login
                        || id == EditorInfo.IME_NULL) {
                    Timber.i("onEditorAction: Form submit request");
                    closeSoftKeyboard();
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        InputFilter[] noSpacesInputFilter = new InputFilter[]{
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
                LinksApi.validateApiUrl(apiUrl);
            } catch (MalformedURLException | InvalidApiUrlException e) {
                Timber.e("attemptLogin: Invalid API URL: " + apiUrlString);
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
            dismissSnackbar();
            doLogin(apiUrl, username, password);
        }
    }

    private void doLogin(final URL apiUrl, final String username, final String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        try {
            mLinkService = new LinksApi(apiUrl).getLinkService(mPreferences);
        } catch (InvalidApiUrlException e) {
            Timber.e("doLogin: ", e);
            showProgress(false);
            mServerAddressView.setError(getString(R.string.error_invalid_server_address));
            return;
        }

        Call<ResponseBody> loginCall = mLinkService.login(loginRequest);
        loginCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Timber.v("doLogin onResponse() called with: " + "response = [" + response + "]");
                Timber.v("doLogin onResponse: Body: " + response.body());
                Timber.v("doLogin onResponse: Error body: " + response.errorBody());
                Gson gson = new Gson();
                try {
                    if (response.isSuccessful()) {
                        // HTTP 200 response, response body might still be empty
                        if (response.body() != null) {
                            try {
                                LoginResponse loginResponse =
                                        gson.fromJson(response.body().string(),
                                                LoginResponse.class);
                                if (!handleLoginSuccess(apiUrl, loginResponse)) {
                                    handleLoginError(true, null, null);
                                }
                            } catch (JsonSyntaxException e) {
                                handleLoginError(false,
                                        null,
                                        getString(R.string.error_invalid_json));
                            }
                        } else {
                            handleLoginError(false,
                                    null,
                                    getString(R.string.error_empty_server_response));
                        }
                    } else {
                        String errorMessage = String.format(getString(R.string.error_http_format),
                                response.code());
                        if (response.errorBody() != null) {
                            try {
                                ErrorResponse errorResponse =
                                        gson.fromJson(response.errorBody().string(),
                                                ErrorResponse.class);
                                if (errorResponse != null) {
                                    Timber.w("doLogin onResponse: API Response message: "
                                            + errorResponse.getErrorMessage());
                                    errorMessage = errorResponse.getErrorMessage();
                                }
                            } catch (JsonSyntaxException ignored) {
                            }
                        }
                        handleLoginError(ResponseUtils.isAuthenticationError(response),
                                null,
                                errorMessage);
                    }
                } catch (IOException e) {
                    Timber.e("doLogin onResponse: ", e);
                    handleLoginError(false, e, null);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Timber.v("doLogin onFailure() called with: " + "t = [" + t + "]");
                Timber.e("doLogin onFailure: ", t);
                handleLoginError(true, t, null);
            }
        });
    }

    private void handleLoginError(
            boolean isUsernameOrPasswordError,
            @Nullable Throwable t,
            @Nullable String errorMessage) {
        Timber.v("handleLoginError() called with: "
                + "isUsernameOrPasswordError = ["
                + isUsernameOrPasswordError
                + "], t = ["
                + t
                + "], errorMessage = ["
                + errorMessage
                + "]");
        showProgress(false);
        String uiMessage;
        if (t != null) {
            String message = t.getLocalizedMessage();
            if (t instanceof UnknownHostException || t instanceof SSLPeerUnverifiedException) {
                mServerAddressView.setError(message);
                mServerAddressView.requestFocus();
            } else {
                showSnackError("Error: " + message, true);
            }
        } else if (isUsernameOrPasswordError) {
            uiMessage = errorMessage != null ? errorMessage
                    : getString(R.string.error_incorrect_username_or_password);
            mPasswordView.setError(uiMessage);
            mPasswordView.requestFocus();
        } else {
            uiMessage = errorMessage != null ? errorMessage : getString(R.string.error_unknown);
            showSnackError(uiMessage, true);
        }
    }

    private boolean handleLoginSuccess(final URL apiUrl, final LoginResponse loginResponse) {
        Timber.v("handleLoginSuccess() called with: "
                + "apiUrl = ["
                + apiUrl
                + "], loginResponse = ["
                + loginResponse
                + "]");
        long userId = loginResponse.getUserId();
        String authString = loginResponse.getAuthString();
        if (!TextUtils.isEmpty(authString) && userId != LoginResponse.INVALID_USER_ID) {
            mPreferences.setApiUrl(apiUrl.toString());
            mPreferences.setUserId(userId);
            mPreferences.setAuthString(authString);
            mPreferences.setUsername(loginResponse.getUsername());
            Timber.i("doLogin Login completed, starting LinksListActivity");
            launchLinksListActivity();
            return true;
        }
        return false;
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= AppPreferences.PASSWORD_MIN_LENGTH;
    }

    private boolean isUsernameValid(String username) {
        return username.length() >= AppPreferences.USERNAME_MIN_LENGTH;
    }

    /**
     * Set up the {@link android.app.ActionBar}
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
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
    }
}

