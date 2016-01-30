package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.models.Link;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by fb on 1/29/16.
 */
public class AddLinkActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AddLink";
    Retrofit mRetrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mRetrofit =
                new Retrofit.Builder().baseUrl(LinkShare.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        super.onCreate(savedInstanceState);
        handleIntent();
    }

    private void handleIntent() {
        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        if (intentReader.isShareIntent()) {
            String htmlText = intentReader.getHtmlText();
            String type = intentReader.getType();
            String text = intentReader.getText().toString();
            // Compose an email
            if (text != null) {
                Log.e(LOG_TAG, "onCreate: text: " + text);
                addLink(text);
            } else {
                showToast("Could not save link");
            }
        }
    }

    public void addLink(String url) {
        Log.v(LOG_TAG, "addLink()");
        LinkShare linkShare = mRetrofit.create(LinkShare.class);
        Call<Link> call = linkShare.addLink(new Link(url));
        Log.i(LOG_TAG, "addLink: Calling URL: " + call.toString());
        call.enqueue(new Callback<Link>() {
            @Override
            public void onResponse(Response<Link> response, Retrofit retrofit) {
                int statusCode = response.code();
                Log.i(LOG_TAG, "onResponse: Response code " + statusCode);
                Log.i(LOG_TAG, "onResponse: Headers: " + response.headers().toString());
                Log.i(LOG_TAG, "onResponse: Body: " + response.message());

                showToast("Link Added Successfully");
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "onResponse: Error during call" + t.getLocalizedMessage());
                showToast(t.getLocalizedMessage());
                finish();
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
