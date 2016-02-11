package com.fernandobarillas.linkshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.LinksList;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;
import com.orm.SugarContext;

import java.net.URL;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends AppCompatActivity {
    private static final String LOG_TAG = "LinksListActivity";
    private static final int GRID_COLUMNS = 1; // How many columns to use when displaying the Links
    AppPreferences mPreferences;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView mRecyclerView;
    LinksAdapter mLinksAdapter;
    LinkShare mLinkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);

        // Set up the service before loading the UI
        mPreferences = new AppPreferences(getApplicationContext());
        serviceSetup(mPreferences.getApiUrl(), mPreferences.getRefreshToken());

        setContentView(R.layout.activity_links_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRecyclerView = (RecyclerView) findViewById(R.id.links_recycler_view);
        final GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SugarContext.terminate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SugarContext.init(this);
        if (LinkStorage.getLinksCount() == 0) {
            getList();
        } else {
            adapterSetup();
        }
    }

    private void archiveLink(final int linkId, final View view) {
        Log.v(LOG_TAG, "archiveLink() called with: " + "linkId = [" + linkId + "]");
        final String errorMessage = "Error deleting " + mLinksAdapter.getUrl(linkId);
        Call<SuccessResponse> deleteCall = mLinkShare.archiveLink(linkId);
        deleteCall.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "onFailure: " + errorMessage, t);
                Snacks.showError(view, errorMessage);
            }

            @Override
            public void onResponse(Response<SuccessResponse> response) {
                Log.v(LOG_TAG, "onResponse() called with: " + "response = [" + response + "]");

                if (response.isSuccess()) {
                    if (response.body().isSuccess()) {
                        Link removedLink = mLinksAdapter.remove(linkId);
                        if (removedLink != null) {
                            mLinksAdapter.notifyItemRemoved(linkId);
                            Snacks.showMessage(view, "Removed " + removedLink.getUrl());
                        }
                        return;
                    }
                }

                Log.e(LOG_TAG, "onResponse: " + errorMessage);
                Log.e(LOG_TAG, String.format("onResponse: %d %s", response.code(), response.message()));
                Snacks.showError(view, errorMessage);
            }
        });
    }

    private void getList() {
        Log.v(LOG_TAG, "getList()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<LinksList> call = mLinkShare.getList();
        call.enqueue(new Callback<LinksList>() {
            @Override
            public void onFailure(Throwable t) {
                Log.v(LOG_TAG, "onFailure() called with: " + "t = [" + t + "]");
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                Snacks.showError(mRecyclerView, errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(Response<LinksList> response) {
                Log.v(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccess()) {
                    String message =
                            "Invalid response returned by server: " + ResponsePrinter.httpCodeString(response);
                    Log.e(LOG_TAG, "onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }

                List<Link> downloadedLinks = response.body().getLinksList();
                if (downloadedLinks == null) {
                    String message = "No links returned by server";
                    Log.e(LOG_TAG, "onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    return;
                }

                // Store the links in the database
                LinkStorage.replaceLinks(downloadedLinks);
                adapterSetup();
                mSwipeRefreshLayout.setRefreshing(false);
                String message =
                        String.format("Downloaded %d %s", LinkStorage.getLinksCount(), LinkStorage.getLinksCount() == 1 ? "link" : "links");
                Log.i(LOG_TAG, "onResponse: " + message);
                Snacks.showMessage(mRecyclerView, message);
            }
        });
    }

    private void adapterSetup() {
        mLinksAdapter = new LinksAdapter(getApplicationContext(), LinkStorage.getAllLinks());
        mRecyclerView.setAdapter(mLinksAdapter);
        touchHelperSetup();
    }

    private Snacks.Action retryGetLinksAction() {
        return new Snacks.Action(R.string.action_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void serviceSetup(URL apiUrl, String refreshToken) {
        Log.v(LOG_TAG, "serviceSetup() called with: " + "refreshToken = [" + refreshToken + "]");
        if (TextUtils.isEmpty(refreshToken)) {
            Log.i(LOG_TAG, "serviceSetup: No refresh token stored, starting LoginActivity");
            launchLoginActivity();
        }
        try {
            mLinkShare = ServiceGenerator.createService(LinkShare.class, apiUrl, refreshToken);
        } catch (InvalidApiUrlException e) {
            Log.e(LOG_TAG, "serviceSetup: Invalid API URL, launching login activity", e);
            launchLoginActivity();
        }
    }

    private void launchLoginActivity() {
        Log.v(LOG_TAG, "launchLoginActivity()");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void touchHelperSetup() {
        Log.v(LOG_TAG, "touchHelperSetup()");
        ItemTouchHelperCallback callback =
                new ItemTouchHelperCallback(new ItemSwipedRightCallback() {
                    @Override
                    public void swipeCallback(RecyclerView.ViewHolder viewHolder) {
                        int linkId = viewHolder.getLayoutPosition();
                        archiveLink(linkId, viewHolder.itemView);
                        // TODO: Bring back swiped item after API error when deleting
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }
}
