package com.fernandobarillas.linkshare.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;

import java.util.List;
import java.util.Set;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends BaseLinkActivity
        implements RealmChangeListener<RealmResults<Link>>,
        NavigationView.OnNavigationItemSelectedListener {

    private static final int CATEGORIES_MENU_GROUP = 2; // Menu Group ID to use for link categories

    private DrawerLayout       mDrawerLayout;
    private NavigationView     mNavigationView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView       mRecyclerView;
    private LinksAdapter       mLinksAdapter;

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
        populateDrawerCategories();
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
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
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
        View header = mNavigationView.getHeaderView(0);
        if (header != null) {
            TextView drawerUsername = (TextView) header.findViewById(R.id.nav_drawer_username);
            if (drawerUsername != null) {
                String title = String.format(getString(R.string.nav_welcome_format),
                        mPreferences.getUsername());
                drawerUsername.setText(title);
                Log.i(LOG_TAG, "onCreate: userdrawer Set drawer text: " + drawerUsername.getText());
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");
        if (mLinksAdapter != null) mLinksAdapter.removeChangeListener();
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.v(LOG_TAG, "onNavigationItemSelected() called with: " + "item = [" + item + "]");
        int id = item.getItemId();
        if (item.getGroupId() == CATEGORIES_MENU_GROUP) {
            Log.i(LOG_TAG, "onNavigationItemSelected: Is checkable " + item.isCheckable());
            String category = item.getTitle().toString();
            Log.d(LOG_TAG, "onNavigationItemSelected: Tapped category: " + category);
            showCategoryLinks(category);
            Log.v(LOG_TAG,
                    "onNavigationItemSelected: Found links count: " + mLinkStorage.findByCategory(
                            category).size());
        } else {
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
                    Toast.makeText(LinksListActivity.this, "Settings coming soon",
                            Toast.LENGTH_SHORT).show();
                    return false;
            }
        }

        closeDrawer();
        return true;
    }

    private void adapterSetup() {
        Log.v(LOG_TAG, "adapterSetup()");
        if (mLinksAdapter == null) {
            showFreshLinks();
        }
        populateDrawerCategories();
        touchHelperSetup();
    }

    private void addLinksChangeListener() {
        if (mLinksAdapter.getLinks() != null) {
            mLinksAdapter.getLinks().removeChangeListener(this);
            mLinksAdapter.getLinks().addChangeListener(this);
        }
    }

    private void archiveLink(final int position) {
        Log.v(LOG_TAG, "archiveLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Log.e(LOG_TAG, "archiveLink: Link instance was null before making delete API call");
            // TODO: Show UI error
            return;
        }

        final String title = link.getTitle();
        final String successMessage = "Archived " + title;
        final String errorMessage = "Error archiving " + title;

        Log.i(LOG_TAG, "archiveLink: Trying to remove link: " + link);
        Call<SuccessResponse> archiveCall = mLinkService.archiveLink(link.getLinkId());
        archiveCall.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> response) {
                Log.v(LOG_TAG, "archiveLink: onResponse() called with: "
                        + "response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    if (mRecyclerView == null) return;
                    Snacks.showMessage(mRecyclerView, successMessage);
                    mLinkStorage.setArchived(link, true);
                    return;
                }

                Log.e(LOG_TAG, "archiveLink: onResponse: " + errorMessage);
                Log.e(LOG_TAG, String.format("archiveLink: onResponse: %d %s", response.code(),
                        response.message()));
                Snacks.showError(mRecyclerView, errorMessage);
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                Log.e(LOG_TAG, "archiveLink: onFailure: " + errorMessage, t);
                Snacks.showError(mRecyclerView, errorMessage);
            }
        });
    }

    private void closeDrawer() {
        Log.v(LOG_TAG, "closeDrawer()");
        if (mDrawerLayout == null) return;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void getList() {
        Log.v(LOG_TAG, "getLinks()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<List<Link>> call = mLinkService.getLinks();
        call.enqueue(new Callback<List<Link>>() {
            @Override
            public void onResponse(Call<List<Link>> call, Response<List<Link>> response) {
                Log.v(LOG_TAG, "getLinks: onResponse: " + ResponsePrinter.httpCodeString(response));
                if (!response.isSuccessful()) {
                    String message = "Invalid response returned by server: "
                            + ResponsePrinter.httpCodeString(response);
                    Log.e(LOG_TAG, "getLinks: onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }

                List<Link> downloadedLinks = response.body();
                if (downloadedLinks == null) {
                    String message = "No links returned by server";
                    Log.e(LOG_TAG, "getLinks: onResponse: " + message);
                    Snacks.showError(mRecyclerView, message, retryGetLinksAction());
                    return;
                }

                // Store the links in the database
                mLinkStorage.replaceLinks(downloadedLinks);
                mSwipeRefreshLayout.setRefreshing(false);
                adapterSetup();
                String message = String.format("Downloaded %d %s", mLinkStorage.getLinksCount(),
                        mLinkStorage.getLinksCount() == 1 ? "link" : "links");
                Log.i(LOG_TAG, "getLinks: onResponse: " + message);
                Snacks.showMessage(mRecyclerView, message);
            }

            @Override
            public void onFailure(Call<List<Link>> call, Throwable t) {
                Log.v(LOG_TAG, "getLinks: onFailure() called with: " + "t = [" + t + "]");
                String errorMessage =
                        "getLinks: onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                Snacks.showError(mRecyclerView, errorMessage);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void populateDrawerCategories() {
        Log.v(LOG_TAG, "populateDrawerCategories()");
        Set<String> categories = mLinkStorage.getCategories();
        if (categories.size() > 0) {
            Menu navMenu = mNavigationView.getMenu();
            navMenu.removeGroup(CATEGORIES_MENU_GROUP); // Avoid duplicate SubMenus
            SubMenu categoriesMenu = navMenu.addSubMenu(CATEGORIES_MENU_GROUP, Menu.NONE, Menu.NONE,
                    getString(R.string.title_categories));
            categoriesMenu.add(CATEGORIES_MENU_GROUP, Menu.NONE, Menu.NONE,
                    getString(R.string.category_uncategorized));
            for (String category : categories) {
                categoriesMenu.add(CATEGORIES_MENU_GROUP, Menu.NONE, Menu.NONE, category);
            }

            // Only allow one category to be checked/highlighted at a time
            categoriesMenu.setGroupCheckable(CATEGORIES_MENU_GROUP, true, true);
        }
    }

    private Snacks.Action retryGetLinksAction() {
        return new Snacks.Action(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void setTitle() {
        if (mLinksAdapter == null) return;
        String categoriesFormat = getString(R.string.title_links_format);
        String title = String.format(categoriesFormat, mToolbarTitle, mLinksAdapter.getItemCount());
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

    private void showCategoryLinks(String category) {
        Log.v(LOG_TAG, "showCategoryLinks() called with: " + "category = [" + category + "]");
        String searchTerm = category;
        if (category.equalsIgnoreCase(getString(R.string.category_uncategorized))) {
            searchTerm = "";
        }
        mLinksAdapter = new LinksAdapter(getApplicationContext(), mLinkStorage,
                mLinkStorage.findByCategory(searchTerm));
        String categoriesFormat = getString(R.string.title_category_links);
        mToolbarTitle = String.format(categoriesFormat, category);
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
                        archiveLink(position);
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
