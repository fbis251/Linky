package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.fernandobarillas.linkshare.api.LinkShare;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class LinksListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    Retrofit mRetrofit;
    TextView mMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_links_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

//    protected void onCreate(Bundle savedInstanceState) {
//        mRetrofit =
//                new Retrofit.Builder().baseUrl(LinkShare.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        Button button = (Button) findViewById(R.id.button);
//        mMessageText = (TextView) findViewById(R.id.message);
//
//        handleIntent();
//
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                addLink("http://example.com");
//            }
//        });
//    }

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
//                setMessage(t.getLocalizedMessage());
            }
        });
    }
}
