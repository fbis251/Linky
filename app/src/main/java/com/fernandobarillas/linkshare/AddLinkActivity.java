package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by fb on 1/29/16.
 */
public class AddLinkActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AddLinkActivity";
    LinkShare mLinkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        AppPreferences mPreferences = new AppPreferences(getApplicationContext());
        serviceSetup(mPreferences.getRefreshToken());
        handleIntent();
    }

    public void addLink(String url) {
        Log.v(LOG_TAG, "addLink() called with: " + "url = [" + url + "]");
        Call<Link> call = mLinkShare.addLink(new Link(url));
        Log.i(LOG_TAG, "addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<Link>() {
            @Override
            public void onFailure(Throwable t) {
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showToast(errorMessage);
                finish();
            }

            @Override
            public void onResponse(Response<Link> response) {
                Log.i(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (response.isSuccess()) {
                    showToast("Link Added Successfully");
                } else {
                    showToast("Error adding link");
                }
                finish();
            }
        });
    }

    private void handleIntent() {
        Log.v(LOG_TAG, "handleIntent()");
        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        if (intentReader.isShareIntent()) {
            String text = intentReader.getText().toString();
            addLink(text);
        }
    }

    private void serviceSetup(String refreshToken) {
        Log.v(LOG_TAG, "serviceSetup() called with: " + "refreshToken = [" + refreshToken + "]");
        mLinkShare = ServiceGenerator.createService(LinkShare.class, refreshToken);
    }

    private void showToast(String message) {
        Log.v(LOG_TAG, "showToast() called with: " + "message = [" + message + "]");
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
