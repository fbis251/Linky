package com.fernandobarillas.linkshare;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.api.LinkShare;
import com.fernandobarillas.linkshare.api.ServiceGenerator;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
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

    private void deleteLink(final int linkId, final View view) {
        Log.v(LOG_TAG, "deleteLink() called with: " + "linkId = [" + linkId + "]");
        final String errorMessage = "Error deleting " + mLinksAdapter.getUrl(linkId);
        Call<SuccessResponse> deleteCall = mLinkShare.deleteLink(linkId);
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
                        String removedUrl = mLinksAdapter.remove(linkId);
                        if (removedUrl != null) {
                            mLinksAdapter.remove(linkId);
                            mLinksAdapter.notifyItemRemoved(linkId);
                            Snacks.showMessage(view, "Removed " + removedUrl);
                        }
                        return;
                    }
                }

                Snacks.showError(view, errorMessage);
            }
        });
    }

    private void getList() {
        Log.v(LOG_TAG, "getList()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<List<String>> call = mLinkShare.getList();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onFailure(Throwable t) {
                Log.v(LOG_TAG, "onFailure() called with: " + "t = [" + t + "]");
                String errorMessage = "onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                Snacks.showError(mRecyclerView, errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(Response<List<String>> response) {
                Log.v(LOG_TAG, "onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccess()) {
                    String message =
                            "Invalid response returned by server: " + ResponsePrinter.httpCodeString(response);
                    Snacks.showError(mRecyclerView, message);
                    Log.e(LOG_TAG, "onResponse: " + message);
                    return;
                }

                List<String> urls = response.body();
                if (urls == null) {
                    String message = "No links returned by server";
                    Snacks.showError(mRecyclerView, message);
                    Log.e(LOG_TAG, "onResponse: " + message);
                    return;
                }

                mLinksAdapter = new LinksAdapter(getApplicationContext(), urls);
                mRecyclerView.setAdapter(mLinksAdapter);
                mSwipeRefreshLayout.setRefreshing(false);
                String message = String.format("Downloaded %d links(s)", urls.size());
                Log.i(LOG_TAG, "onResponse: " + message);
                Snacks.showMessage(mRecyclerView, message);
                touchHelperSetup();
            }
        });
    }

    private void serviceSetup(String refreshToken) {
        Log.v(LOG_TAG, "serviceSetup() called with: " + "refreshToken = [" + refreshToken + "]");
        mLinkShare = ServiceGenerator.createService(LinkShare.class, refreshToken);
    }

    private void touchHelperSetup() {
        Log.v(LOG_TAG, "touchHelperSetup()");
        ItemTouchHelperCallback callback =
                new ItemTouchHelperCallback(new ItemSwipedRightCallback() {
                    @Override
                    public void swipeCallback(RecyclerView.ViewHolder viewHolder) {
                        int linkId = viewHolder.getLayoutPosition();
                        deleteLink(linkId, viewHolder.itemView);
                        // TODO: Bring back swiped item after API error when deleting
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }
}
