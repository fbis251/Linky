package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
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
        super.onCreate(savedInstanceState);
        serviceSetup();
        handleIntent();
    }

    private void serviceSetup() {
        // TODO: Load from sharedpreferences
        String username = "";
        String password = "";
        mLinkShare = ServiceGenerator.createService(LinkShare.class, username, password);
    }

    private void handleIntent() {
        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        if (intentReader.isShareIntent()) {
            String text = intentReader.getText().toString();
            Log.e(LOG_TAG, "onCreate: text: " + text);
            addLink(text);
        }
    }

    public void addLink(String url) {
        Log.v(LOG_TAG, "addLink() called with: " + "url = [" + url + "]");
        Call<Link> call = mLinkShare.addLink(new Link(url));
        Log.i(LOG_TAG, "addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<Link>() {
            @Override
            public void onResponse(Response<Link> response) {
                int statusCode = response.code();
                Log.i(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                showToast("Link Added Successfully");
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showToast(errorMessage);
                finish();
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
