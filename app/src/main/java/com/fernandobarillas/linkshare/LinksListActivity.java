package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.fernandobarillas.linkshare.adapters.LinksAdapter;
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
    RecyclerView mRecyclerView;
    LinksAdapter mLinksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mRetrofit =
                new Retrofit.Builder().baseUrl(LinkShare.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_links_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        getLinks();
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
                mLinksAdapter = new LinksAdapter(getApplicationContext(), urls);
                mRecyclerView.setAdapter(mLinksAdapter);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "onResponse: Error during call" + t.getLocalizedMessage());
                showToast(t.getLocalizedMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
