package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.activities.BaseLinkActivity;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.LinksList;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends BaseLinkActivity {
    private static final int GRID_COLUMNS = 1; // How many columns when displaying the Links

    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView       mRecyclerView;
    LinksAdapter       mLinksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceSetup();
        setContentView(R.layout.activity_links_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRecyclerView = (RecyclerView) findViewById(R.id.links_recycler_view);
        final GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getList();
                }
            });
        }

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
        Log.v(LOG_TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "onResume()");
        super.onResume();
        if (mLinkStorage.getLinksCount() == 0) {
            getList();
        } else {
            adapterSetup();
        }
    }

    private void adapterSetup() {
        Log.v(LOG_TAG, "adapterSetup()");
        if (mLinksAdapter == null) {
            mLinksAdapter = new LinksAdapter(getApplicationContext(), mLinkStorage);
            mRecyclerView.setAdapter(mLinksAdapter);
        }
        Log.v(LOG_TAG, "adapterSetup: Count: " + mLinksAdapter.getItemCount());
        setToolbarTitle("Saved Links: " + mLinksAdapter.getItemCount());
        touchHelperSetup();
    }

    private void deleteLink(final int linkId) {
        Log.v(LOG_TAG, "deleteLink() called with: " + "linkId = [" + linkId + "]");

        final String errorMessage = "Error deleting " + mLinksAdapter.getUrl(linkId);

        final Link removedLink = mLinksAdapter.remove(linkId);
        mLinksAdapter.notifyItemRemoved(linkId);

        Call<SuccessResponse> deleteCall = mLinkService.deleteLink(removedLink.getLinkId());
        deleteCall.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> response) {
                Log.v(LOG_TAG, "onResponse() called with: " + "response = [" + response + "]");

                SuccessResponse archiveResponse = response.body();
                if (response.isSuccessful() && archiveResponse.isSuccess()) {
                    if (mRecyclerView == null) return;
                    Snacks.showMessage(mRecyclerView, "Removed " + removedLink.getUrl());
                    return;
                }

                Log.e(LOG_TAG, "onResponse: " + errorMessage);
                Log.e(LOG_TAG,
                        String.format("onResponse: %d %s", response.code(), response.message()));
                Snacks.showError(mRecyclerView, errorMessage);
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                Log.e(LOG_TAG, "onFailure: " + errorMessage, t);
                Snacks.showError(mRecyclerView, errorMessage);
            }
        });
    }

    private void getList() {
        Log.v(LOG_TAG, "getList()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<LinksList> call = mLinkService.getList();
        call.enqueue(new Callback<LinksList>() {
            @Override
            public void onResponse(Call<LinksList> call, Response<LinksList> response) {
                Log.v(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccessful()) {
                    String message = "Invalid response returned by server: "
                            + ResponsePrinter.httpCodeString(response);
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
                mLinkStorage.replaceLinks(downloadedLinks);
                mLinksAdapter = null;
                adapterSetup();
                mSwipeRefreshLayout.setRefreshing(false);
                String message = String.format("Downloaded %d %s", mLinkStorage.getLinksCount(),
                        mLinkStorage.getLinksCount() == 1 ? "link" : "links");
                Log.i(LOG_TAG, "onResponse: " + message);
                Snacks.showMessage(mRecyclerView, message);
            }

            @Override
            public void onFailure(Call<LinksList> call, Throwable t) {
                Log.v(LOG_TAG, "onFailure() called with: " + "t = [" + t + "]");
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                Snacks.showError(mRecyclerView, errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private Snacks.Action retryGetLinksAction() {
        return new Snacks.Action(R.string.action_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void touchHelperSetup() {
        Log.v(LOG_TAG, "touchHelperSetup()");
        ItemTouchHelperCallback callback =
                new ItemTouchHelperCallback(new ItemSwipedRightCallback() {
                    @Override
                    public void swipeCallback(RecyclerView.ViewHolder viewHolder) {
                        int linkId = viewHolder.getLayoutPosition();
                        deleteLink(linkId);
                        // TODO: Bring back swiped item after API error when deleting
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }
}
