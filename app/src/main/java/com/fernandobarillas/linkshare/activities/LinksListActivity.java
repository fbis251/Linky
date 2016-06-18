package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.LinksList;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends BaseLinkActivity
        implements RealmChangeListener<RealmResults<Link>>,
        NavigationView.OnNavigationItemSelectedListener {
    private static final int GRID_COLUMNS = 1; // How many columns when displaying the Links

    private DrawerLayout       mDrawerLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView       mRecyclerView;
    private LinksAdapter       mLinksAdapter;
    private TextView           mDrawerUsername;

    private String mToolbarTitle;

    @Override
    public void onBackPressed() {
        if (mDrawerLayout == null) super.onBackPressed();
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "onResume()");
        super.onResume();
        if (mLinksAdapter == null) {
            showFreshLinks();
        }
        if (mLinkStorage.getLinksCount() == 0) {
            getList();
        } else {
            adapterSetup();
        }
    }

    @Override
    public void onChange(RealmResults<Link> element) {
        setTitle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceSetup();

        setContentView(R.layout.activity_links_list);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.links_drawer_layout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.links_swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.links_recycler_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mToolbarTitle = getString(R.string.title_fresh_links);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getList();
            }
        });

        // Set up the drawer's username
        View header = navigationView.getHeaderView(0);
        if (header != null) {
            mDrawerUsername = (TextView) header.findViewById(R.id.nav_drawer_username);
            Log.i(LOG_TAG, "onCreate: userdrawer: " + mDrawerUsername);
            Log.i(LOG_TAG, "onCreate: userdrawer name: " + mPreferences.getUsername());
            if (mDrawerUsername != null) {
                // FIXME: The drawer textview is null
                mDrawerUsername.setText(mPreferences.getUsername());
                Log.i(LOG_TAG,
                        "onCreate: userdrawer Set drawer text: " + mDrawerUsername.getText());
            } else {
                Log.e(LOG_TAG, "onCreate: userdrawer null");
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");
        mLinksAdapter.removeChangeListener();
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (R.id.nav_fresh_links):
                Log.i(LOG_TAG, "onNavigationItemSelected: Displaying fresh Links");
                showFreshLinks();
                break;
            case (R.id.nav_all_links):
                Log.i(LOG_TAG, "onNavigationItemSelected: Displaying all Links");
                showAllLinks();
                break;
            case (R.id.nav_favorites):
                Log.i(LOG_TAG, "onNavigationItemSelected: Displaying favorite Links");
                showFavoriteLinks();
                break;
            case (R.id.nav_archived):
                Log.i(LOG_TAG, "onNavigationItemSelected: Displaying archived Links");
                showArchivedLinks();
                break;
            case (R.id.nav_settings):
                Log.i(LOG_TAG, "onNavigationItemSelected: Opening Settings");
                Toast.makeText(LinksListActivity.this, "Settings coming soon", Toast.LENGTH_SHORT)
                        .show();
                return false;
        }

        closeDrawer();
        return true;
    }

    private void adapterSetup() {
        Log.v(LOG_TAG, "adapterSetup()");
        if (mLinksAdapter == null) {
            showFreshLinks();
        }
        touchHelperSetup();
    }

    private void addLinksChangeListener() {
        if (mLinksAdapter.getLinks() != null) {
            mLinksAdapter.getLinks().removeChangeListener(this);
            mLinksAdapter.getLinks().addChangeListener(this);
        }
    }

    private void closeDrawer() {
        Log.v(LOG_TAG, "closeDrawer()");
        if (mDrawerLayout == null) return;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void deleteLink(final int position) {
        Log.v(LOG_TAG, "deleteLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Log.e(LOG_TAG, "deleteLink: Link instance was null before making delete API call");
            // TODO: Show UI error
            return;
        }

        final String url = link.getUrl();
        final String successMessage = "Removed " + url;
        final String errorMessage = "Error deleting " + url;

        Log.i(LOG_TAG, "deleteLink: Trying to remove link: " + link);
        Call<SuccessResponse> deleteCall = mLinkService.deleteLink(link.getLinkId());
        deleteCall.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> response) {
                Log.v(LOG_TAG,
                        "deleteLink: onResponse() called with: " + "response = [" + response + "]");

                SuccessResponse archiveResponse = response.body();
                if (response.isSuccessful() && archiveResponse.isSuccess()) {
                    if (mRecyclerView == null) return;
                    Snacks.showMessage(mRecyclerView, successMessage);
                    mLinkStorage.remove(link);
                    return;
                }

                Log.e(LOG_TAG, "deleteLink: onResponse: " + errorMessage);
                Log.e(LOG_TAG, String.format("deleteLink: onResponse: %d %s", response.code(),
                        response.message()));
                Snacks.showError(mRecyclerView, errorMessage);
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                Log.e(LOG_TAG, "deleteLink: onFailure: " + errorMessage, t);
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
                Log.v(LOG_TAG, "getList: onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccessful()) {
                    String message = "Invalid response returned by server: "
                            + ResponsePrinter.httpCodeString(response);
                    Log.e(LOG_TAG, "getList: onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }

                List<Link> downloadedLinks = response.body().getLinksList();
                if (downloadedLinks == null) {
                    String message = "No links returned by server";
                    Log.e(LOG_TAG, "getList: onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    return;
                }

                // Store the links in the database
                mLinkStorage.replaceLinks(downloadedLinks);
                mSwipeRefreshLayout.setRefreshing(false);
                String message = String.format("Downloaded %d %s", mLinkStorage.getLinksCount(),
                        mLinkStorage.getLinksCount() == 1 ? "link" : "links");
                Log.i(LOG_TAG, "getList: onResponse: " + message);
                Snacks.showMessage(mRecyclerView, message);
            }

            @Override
            public void onFailure(Call<LinksList> call, Throwable t) {
                Log.v(LOG_TAG, "getList: onFailure() called with: " + "t = [" + t + "]");
                String errorMessage =
                        "getList: onFailure: Error during call: " + t.getLocalizedMessage();
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

    private void setTitle() {
        if (mLinksAdapter == null) return;
        String title = String.format("%s: %d", mToolbarTitle, mLinksAdapter.getItemCount());
        Log.i(LOG_TAG, "setTitle: New Title: " + title);
        setToolbarTitle(title);
    }

    private void showAllLinks() {
        mLinksAdapter =
                new LinksAdapter(getApplicationContext(), mLinkStorage, mLinkStorage.getAllLinks());
        mToolbarTitle = getString(R.string.title_all_links);
        updateUiAfterAdapterChange();
    }

    private void showArchivedLinks() {
        mLinksAdapter = new LinksAdapter(getApplicationContext(), mLinkStorage,
                mLinkStorage.getAllArchived());
        mToolbarTitle = getString(R.string.title_archived_links);
        updateUiAfterAdapterChange();
    }

    private void showFavoriteLinks() {
        mLinksAdapter = new LinksAdapter(getApplicationContext(), mLinkStorage,
                mLinkStorage.getAllFavorites());
        mToolbarTitle = getString(R.string.title_favorite_links);
        updateUiAfterAdapterChange();
    }

    private void showFreshLinks() {
        mLinksAdapter = new LinksAdapter(getApplicationContext(), mLinkStorage,
                mLinkStorage.getAllFreshLinks());
        mToolbarTitle = getString(R.string.title_fresh_links);
        updateUiAfterAdapterChange();
    }

    private void touchHelperSetup() {
        Log.v(LOG_TAG, "touchHelperSetup()");
        ItemTouchHelperCallback callback =
                new ItemTouchHelperCallback(new ItemSwipedRightCallback() {
                    @Override
                    public void swipeCallback(RecyclerView.ViewHolder viewHolder) {
                        int position = viewHolder.getLayoutPosition();
                        deleteLink(position);
                        // TODO: Swipe should archive the Link, not delete
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void updateUiAfterAdapterChange() {
        if (mLinksAdapter != null) {
            mRecyclerView.setAdapter(mLinksAdapter);
        }
        setTitle();
        addLinksChangeListener();
    }
}
