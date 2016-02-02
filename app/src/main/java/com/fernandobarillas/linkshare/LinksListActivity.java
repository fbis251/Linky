package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    AppPreferences mPreferences;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView mRecyclerView;
    LinksAdapter mLinksAdapter;
    LinkShare mLinkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
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
                getList();
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.links_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getList();
            }
        });

        mPreferences = new AppPreferences(getApplicationContext());

        serviceSetup(mPreferences.getRefreshToken());
        getList();
    }

    private void serviceSetup(String refreshToken) {
        Log.v(LOG_TAG, "serviceSetup() called with: " + "refreshToken = [" + refreshToken + "]");
        mLinkShare = ServiceGenerator.createService(LinkShare.class, refreshToken);
    }

    private void getList() {
        Log.v(LOG_TAG, "getList()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<List<String>> call = mLinkShare.getList();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Response<List<String>> response) {
                Log.v(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccess()) {
                    String message =
                            "Invalid response returned by server: " + ResponsePrinter.httpCodeString(response);
                    showError(message);
                    Log.e(LOG_TAG, "onResponse: " + message);
                    return;
                }

                List<String> urls = response.body();
                if (urls == null) {
                    String message = "No links returned by server";
                    showError(message);
                    Log.e(LOG_TAG, "onResponse: " + message);
                    return;
                }

                mLinksAdapter = new LinksAdapter(getApplicationContext(), urls);
                mRecyclerView.setAdapter(mLinksAdapter);
                mSwipeRefreshLayout.setRefreshing(false);
                Log.i(LOG_TAG, "onResponse: Got " + urls.size() + " urls");
                showSnackBar(String.format("Downloaded %d links(s)", urls.size()));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.v(LOG_TAG, "onFailure() called with: " + "t = [" + t + "]");
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showError(errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showError(String message) {
        mSwipeRefreshLayout.setRefreshing(false);
        showSnackBar(message, true);
    }

    private void showSnackBar(String message) {
        showSnackBar(message, false);
    }

    private void showSnackBar(String message, boolean indefinite) {
        Log.v(LOG_TAG, "showSnackBar() called with: " + "message = [" + message + "], indefinite = [" + indefinite + "]");
        int length = indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;
        Snackbar.make(mRecyclerView, message, length).setAction("Action", null).show();
    }
}
