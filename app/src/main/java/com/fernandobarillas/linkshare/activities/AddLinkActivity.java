package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.widget.Toast;

import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by fb on 1/29/16.
 */
public class AddLinkActivity extends BaseLinkActivity {
    private static final String LOG_TAG = "AddLinkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG,
                "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        serviceSetup();
        handleIntent();
    }

    public void addLink(final String title, final String url) {
        Log.v(LOG_TAG, "addLink() called with: " + "title = [" + title + "], url = [" + url + "]");

        final Link newLink = new Link(url);
        newLink.setTitle(title);
        newLink.setCategory("");
        newLink.setTimestamp(new Date());

        AddLinkRequest linkRequest = new AddLinkRequest(newLink);

        Call<Link> call = mLinkService.addLink(linkRequest);
        Log.i(LOG_TAG, "addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<Link>() {
            @Override
            public void onResponse(Call<Link> call, Response<Link> response) {
                Log.i(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (response.isSuccessful()) {
//                    Link newLink = response.body(); // TODO: Get the link from API response
                    mLinkStorage.add(newLink, true);
                    Log.i(LOG_TAG, "onResponse: Added Link: " + newLink);
                    String message = String.format("Added %s",
                            (newLink.getTitle() != null) ? newLink.getTitle()
                                    : newLink.getDomain());
                    showToast(message);
                } else {
                    showToast("Error adding link");
                }
                finish();
            }

            @Override
            public void onFailure(Call<Link> call, Throwable t) {
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showToast(errorMessage);
                finish();
            }
        });
    }

    private void handleIntent() {
        Log.v(LOG_TAG, "handleIntent()");
        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        if (intentReader.isShareIntent()) {
            String title = intentReader.getSubject();
            String text = intentReader.getText().toString();
            addLink(title, text);
        }
    }

    private void showToast(String message) {
        Log.v(LOG_TAG, "showToast() called with: " + "message = [" + message + "]");
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
