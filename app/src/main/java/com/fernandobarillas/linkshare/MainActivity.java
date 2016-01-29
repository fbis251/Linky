package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.models.Link;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    Retrofit mRetrofit;
    TextView mMessageText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mRetrofit =
                new Retrofit.Builder().baseUrl(LinkShare.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        mMessageText = (TextView) findViewById(R.id.message);

        handleIntent();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLink("http://example.com");
            }
        });
    }

    private void handleIntent() {
        ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
        if (intentReader.isShareIntent()) {
            String htmlText = intentReader.getHtmlText();
            String type = intentReader.getType();
            String text = intentReader.getText().toString();
            // Compose an email
            if (type != null) {
                Log.e(LOG_TAG, "onCreate: type: " + type);
            }
            if (htmlText != null) {
                Log.e(LOG_TAG, "onCreate: html: " + htmlText);
            }
            if (text != null) {
                Log.e(LOG_TAG, "onCreate: text: " + text);
                addLink(text);
            } else {

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

                Toast.makeText(getApplicationContext(), "Link added", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "onResponse: Error during call" + t.getLocalizedMessage());
                setMessage(t.getLocalizedMessage());
            }
        });
    }

    private void getLinks() {
        mRetrofit =
                new Retrofit.Builder().baseUrl(LinkShare.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        LinkShare linkShare = mRetrofit.create(LinkShare.class);
        Call<List<String>> call = linkShare.getList();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Response<List<String>> response, Retrofit retrofit) {
                int statusCode = response.code();
                Log.i(LOG_TAG, "onResponse: Response code " + statusCode);
                List<String> urls = response.body();
                Log.e(LOG_TAG, "onResponse: Got " + urls.size() + " urls");
                for (String url : urls) {
                    Log.e(LOG_TAG, "" + url);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "onResponse: Error during call" + t.getLocalizedMessage());
                setMessage(t.getLocalizedMessage());
            }
        });
    }

    private void setMessage(String message) {
        if(mMessageText != null && message != null) {
            mMessageText.setText(message);
        }
    }
}
