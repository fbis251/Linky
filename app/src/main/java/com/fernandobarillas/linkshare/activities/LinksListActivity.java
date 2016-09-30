package com.fernandobarillas.linkshare.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponsePrinter;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.bottomsheet.BottomSheetListener;

import java.util.List;
import java.util.Set;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinksListActivity extends BaseLinkActivity
        implements RealmChangeListener<RealmResults<Link>>,
        NavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener {

    /** An invalid position for a Link within an Adapter */
    public static final int INVALID_LINK_POSITION = -1;

    private static final int EDIT_LINK_REQUEST     = 1;// Request code for EditLinkActivity
    private static final int CATEGORIES_MENU_GROUP = 2; // Menu Group ID to use for link categories

    // Clipboard handling
    private static final String CLIPBOARD_LABEL = "LINK_URL";

    // Bundle instance saving
    private static final String STATE_CATEGORY             = "category";
    private static final String STATE_SEARCH_TERM          = "searchTerm";
    private static final String STATE_FILTER_MODE          = "filterMode";
    private static final String STATE_PREVIOUS_FILTER_MODE = "previousFilterMode";
    private static final String STATE_SORT_MODE            = "sortMode";

    private DrawerLayout       mDrawerLayout;
    private TextView           mDrawerUsername;
    private NavigationView     mNavigationView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView       mRecyclerView;
    private LinksAdapter       mLinksAdapter;
    private SearchView         mSearchView;

    // Sorting and filtering for results
    private String mCategory;
    private String mSearchTerm;
    @LinkStorage.FilterMode
    private int    mPreviousFilterMode;
    @LinkStorage.FilterMode
    private int    mFilterMode;
    @LinkStorage.SortMode
    private int    mSortMode;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(LOG_TAG, "onActivityResult() called with: "
                + "requestCode = ["
                + requestCode
                + "], resultCode = ["
                + resultCode
                + "], data = ["
                + data
                + "]");
        if (requestCode == EDIT_LINK_REQUEST && resultCode == RESULT_OK) {
            // We can't reliably tell what operation the user performed on the Link, so it's best
            // to just tell the adapter the entire dataset changed. Examples of not being able
            // to tell include setting favorite to false when browsing favorites
            if (mLinksAdapter != null) mLinksAdapter.notifyDataSetChanged();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

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
        if (mLinkStorage.getLinksCount() == 0) {
            getList();
        } else {
            adapterSetup();
        }
    }

    @Override
    public void onChange(RealmResults<Link> element) {
        Log.v(LOG_TAG, "onChange() called with: " + "element = [" + element + "]");
        populateDrawerCategories();
        updateToolbarTitle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceSetup();

        mCategory = null;
        mSearchTerm = null;
        mFilterMode = LinkStorage.FILTER_FRESH;
        mSortMode = LinkStorage.SORT_TIMESTAMP_DESCENDING;

        if (savedInstanceState != null) {
            mCategory = savedInstanceState.getString(STATE_CATEGORY);
            mSearchTerm = savedInstanceState.getString(STATE_SEARCH_TERM);
            //noinspection WrongConstant
            mFilterMode = savedInstanceState.getInt(STATE_FILTER_MODE, LinkStorage.FILTER_FRESH);
            //noinspection WrongConstant
            mPreviousFilterMode =
                    savedInstanceState.getInt(STATE_PREVIOUS_FILTER_MODE, LinkStorage.FILTER_FRESH);
            //noinspection WrongConstant
            mSortMode = savedInstanceState.getInt(STATE_SORT_MODE,
                    LinkStorage.SORT_TIMESTAMP_DESCENDING);
        }

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

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);
            showNavAccountMenu(false);
        }

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
            mDrawerUsername = (TextView) header.findViewById(R.id.nav_drawer_username);
            if (mDrawerUsername != null) {
                String title = String.format(getString(R.string.nav_welcome_format),
                        mPreferences.getUsername());
                mDrawerUsername.setText(title);
                Log.i(LOG_TAG,
                        "onCreate: userdrawer Set drawer text: " + mDrawerUsername.getText());
                mDrawerUsername.setOnClickListener(navAccountShowListener());
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");

        if (mLinksAdapter != null && mLinksAdapter.getLinks() != null) {
            mLinksAdapter.getLinks().removeChangeListeners();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(LOG_TAG, "onCreateOptionsMenu() called with: " + "menu = [" + menu + "]");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_links_list, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = null;
        if (searchItem != null) {
            mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }
        if (mSearchView != null) {
            mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) closeSoftKeyboard(view);
                }
            });
            mSearchView.setOnQueryTextListener(this);
            // Not sure why passing in mSearchTerm into setQuery() below isn't working
            // Creating a copy of the String inside the if block below didn't work either
            final String searchTerm = mSearchTerm;
            if (!TextUtils.isEmpty(searchTerm)) {
                Log.v(LOG_TAG, "onCreateOptionsMenu: searchTerm = [" + searchTerm + "]");
                mSearchView.onActionViewExpanded();
                mSearchView.setQuery(searchTerm, false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(LOG_TAG, "onOptionsItemSelected() called with: " + "item = [" + item + "]");
        int id = item.getItemId();

        // Keep track of the current SortMode to update the UI if it changes
        int lastSortMode = mSortMode;
        switch (id) {
            case (R.id.sort_title_ascending):
                Log.d(LOG_TAG, "onOptionsItemSelected: Title Ascending");
                mSortMode = LinkStorage.SORT_TITLE_ASCENDING;
                break;
            case (R.id.sort_title_descending):
                Log.d(LOG_TAG, "onOptionsItemSelected: Title Descending");
                mSortMode = LinkStorage.SORT_TITLE_DESCENDING;
                break;
            case (R.id.sort_timestamp_ascending):
                Log.d(LOG_TAG, "onOptionsItemSelected: Timestamp Ascending");
                mSortMode = LinkStorage.SORT_TIMESTAMP_ASCENDING;
                break;
            case (R.id.sort_timestamp_descending):
                Log.d(LOG_TAG, "onOptionsItemSelected: Timestamp Descending");
                mSortMode = LinkStorage.SORT_TIMESTAMP_DESCENDING;
                break;
            default:
                break;
        }

        if (lastSortMode != mSortMode) {
            updateUiAfterFilterModeChange();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.v(LOG_TAG, "onNavigationItemSelected() called with: " + "item = [" + item + "]");
        int id = item.getItemId();

        // Keep track of the current filter mode to update the UI if it changes
        int lastFilterMode = mFilterMode;
        if (item.getGroupId() == R.id.nav_drawer_account) {
            Log.v(LOG_TAG, "onNavigationItemSelected: item = [" + item + "]");
            performLogout();
        } else if (item.getGroupId() == CATEGORIES_MENU_GROUP) {
            mFilterMode = LinkStorage.FILTER_CATEGORY;
            mCategory = item.getTitle().toString();
            Log.d(LOG_TAG, "onNavigationItemSelected: Tapped category: " + mCategory);
        } else {
            switch (id) {
                case (R.id.nav_fresh_links):
                    Log.i(LOG_TAG, "onNavigationItemSelected: Displaying fresh Links");
                    mFilterMode = LinkStorage.FILTER_FRESH;
                    break;
                case (R.id.nav_all_links):
                    Log.i(LOG_TAG, "onNavigationItemSelected: Displaying all Links");
                    mFilterMode = LinkStorage.FILTER_ALL;
                    break;
                case (R.id.nav_favorites):
                    Log.i(LOG_TAG, "onNavigationItemSelected: Displaying favorite Links");
                    mFilterMode = LinkStorage.FILTER_FAVORITES;
                    break;
                case (R.id.nav_archived):
                    Log.i(LOG_TAG, "onNavigationItemSelected: Displaying archived Links");
                    mFilterMode = LinkStorage.FILTER_ARCHIVED;
                    break;
                case (R.id.nav_settings):
                    Log.i(LOG_TAG, "onNavigationItemSelected: Opening Settings");
                    Toast.makeText(LinksListActivity.this, "Settings coming soon",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AboutActivity.class));
                    return false;
                default:
                    break;
            }
        }

        if (lastFilterMode != mFilterMode) updateUiAfterFilterModeChange();
        closeDrawer();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.v(LOG_TAG, "onQueryTextSubmit() called with: " + "query = [" + query + "]");
        handleSearch(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.v(LOG_TAG, "onQueryTextChange() called with: " + "newText = [" + newText + "]");
        handleSearch(newText);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(LOG_TAG, "onSaveInstanceState() called with: " + "outState = [" + outState + "]");
        outState.putString(STATE_SEARCH_TERM, mSearchTerm);
        outState.putString(STATE_CATEGORY, mCategory);
        outState.putInt(STATE_FILTER_MODE, mFilterMode);
        outState.putInt(STATE_PREVIOUS_FILTER_MODE, mPreviousFilterMode);
        outState.putInt(STATE_SORT_MODE, mSortMode);
        Log.d(LOG_TAG, "onSaveInstanceState() returned: " + outState);
        super.onSaveInstanceState(outState);
    }

    public void copyLink(final int position) {
        Log.v(LOG_TAG, "copyUrl() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Log.e(LOG_TAG, "copyUrl: Link instance was null before copying URL");
            showSnackError("Could not copy link URL, please refresh", getRefreshSnackAction());
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(CLIPBOARD_LABEL, link.getUrl());
        clipboard.setPrimaryClip(clipData);
        showSnackSuccess("Copied URL: " + link.getUrl());
    }

    public void deleteLink(final int position) {
        Log.v(LOG_TAG, "deleteLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Log.e(LOG_TAG, "deleteLink: Link instance was null before making delete API call");
            showSnackError("Could not delete link, please refresh", getRefreshSnackAction());
            return;
        }

        // TODO: maybe add confirmation popup or some UI confirmation element?
        final String title = link.getTitle();
        final String successMessage = "Deleted " + title;
        final String errorMessage = "Error deleting " + title;

        Log.i(LOG_TAG, "deleteLink: Trying to remove link: " + link);
        Call<SuccessResponse> deleteCall = mLinkService.deleteLink(link.getLinkId());
        deleteCall.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> response) {
                Log.v(LOG_TAG,
                        "deleteLink: onResponse() called with: " + "response = [" + response + "]");
                if (response.isSuccessful()) {
                    if (mRecyclerView == null) return;
                    showSnackSuccess(successMessage);
                    if (mLinkStorage != null) mLinkStorage.remove(link);
                    if (mLinksAdapter != null) {
                        mLinksAdapter.notifyItemRemoved(position);
                    }
                    return;
                }

                Log.e(LOG_TAG, "deleteLink: onResponse: " + errorMessage);
                Log.e(LOG_TAG, String.format("deleteLink: onResponse: %d %s", response.code(),
                        response.message()));
                showSnackError(errorMessage, false);
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                Log.e(LOG_TAG, "deleteLink: onFailure: " + errorMessage, t);
                showSnackError(errorMessage, false);
            }
        });
    }

    public void displayBottomSheet(final int position) {
        Log.v(LOG_TAG, "displayBottomSheet() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        if (link == null) return;
        new BottomSheet.Builder(this).setSheet(R.menu.link_options)
                .setTitle(link.getTitle())
                .setListener(new BottomSheetLinkListener(position))
                .show();
    }

    public void editLink(final int position) {
        Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_edit), getRefreshSnackAction());
            return;
        }
        Intent editIntent = new Intent(getApplicationContext(), EditLinkActivity.class);
        editIntent.putExtra(EditLinkActivity.EXTRA_LINK_ID, link.getLinkId());
        startActivityForResult(editIntent, EDIT_LINK_REQUEST);
    }

    public void openLink(final int position) {
        Log.v(LOG_TAG, "openLink() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        Log.i(LOG_TAG, "openLink: Link: " + link);
        if (link == null) {
            Log.e(LOG_TAG, "openLink: Null Link when attempting to open URL: " + position);
            showSnackError(getString(R.string.error_link_null), false);
            return;
        }

        String url = link.getUrl();
        if (TextUtils.isEmpty(url)) {
            Log.e(LOG_TAG, "openLink: Cannot open empty or null link");
            showSnackError(getString(R.string.error_link_url_null), false);
        }
        Log.i(LOG_TAG, "openLink: Opening URL: " + url);
        openUrlExternally(url);
    }

    public void setFavorite(final int position, final boolean isFavorite) {
        Log.v(LOG_TAG, "setFavorite() called with: "
                + "position = ["
                + position
                + "], isFavorite = ["
                + isFavorite
                + "]");
        final Link link = mLinksAdapter.getLink(position);
        Log.i(LOG_TAG, "setFavorite: Link: " + link);
        if (link == null) {
            Log.e(LOG_TAG, "openLink: Null Link when attempting to favorite position " + position);
            showSnackError(getString(R.string.error_link_favorite), false);
            return;
        }

        // TODO: Extract strings
        final String errorMessage =
                String.format("API Error %s favorite: %s", isFavorite ? "adding" : "removing",
                        link.getTitle());

        final Snacks.Action retryAction =
                new Snacks.Action(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v(LOG_TAG, "setFavorite retryAction onClick() called with: "
                                + "view = ["
                                + view
                                + "]");
                        setFavorite(position, isFavorite);
                    }
                });

        Call<SuccessResponse> call = mLinkService.favoriteLink(link.getLinkId());
        if (!isFavorite) {
            call = mLinkService.unfavoriteLink(link.getLinkId());
        }

        call.enqueue(new Callback<SuccessResponse>() {
            @Override
            public void onResponse(Call<SuccessResponse> call, Response<SuccessResponse> response) {
                Log.v(LOG_TAG, "setFavorite onResponse() called with: "
                        + "call = ["
                        + call
                        + "], response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "setFavorite onResponse: Successful");
                    if (mLinkStorage != null) mLinkStorage.setFavorite(link, isFavorite);
                    if (mLinksAdapter != null) {
                        if (mFilterMode == LinkStorage.FILTER_FAVORITES && !isFavorite) {
                            mLinksAdapter.notifyItemRemoved(position);
                        } else {
                            mLinksAdapter.notifyItemChanged(position);
                        }
                    }
                } else {
                    showSnackError(errorMessage, retryAction);
                }
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                showSnackError(errorMessage, retryAction);
            }
        });
    }

    public void shareLink(final int position) {
        Log.v(LOG_TAG, "shareLink() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_edit), getRefreshSnackAction());
            return;
        }
        shareUrl(link.getTitle(), link.getUrl());
    }

    private void adapterSetup() {
        Log.v(LOG_TAG, "adapterSetup()");
        if (mLinksAdapter == null) {
            showAllLinks();
        }
        populateDrawerCategories();
        touchHelperSetup();
    }

    private void addLinksChangeListener() {
        if (mLinksAdapter != null && mLinksAdapter.getLinks() != null) {
            mLinksAdapter.getLinks().removeChangeListeners();
            mLinksAdapter.getLinks().addChangeListener(this);
        }
    }

    private void archiveLink(final int position) {
        Log.v(LOG_TAG, "archiveLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Log.e(LOG_TAG, "archiveLink: Link instance was null before making archive API call");
            showSnackError("Could not archive link, please refresh", getRefreshSnackAction());
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
                    showSnackSuccess(successMessage);
                    if (mLinkStorage != null) mLinkStorage.setArchived(link, true);
                    if (mLinksAdapter != null) {
                        if (mFilterMode == LinkStorage.FILTER_FRESH) {
                            mLinksAdapter.notifyItemRemoved(position);
                        } else {
                            mLinksAdapter.notifyItemChanged(position);
                        }
                    }
                    return;
                }

                Log.e(LOG_TAG, "archiveLink: onResponse: " + errorMessage);
                Log.e(LOG_TAG, String.format("archiveLink: onResponse: %d %s", response.code(),
                        response.message()));
                showSnackError(errorMessage, false);
            }

            @Override
            public void onFailure(Call<SuccessResponse> call, Throwable t) {
                Log.e(LOG_TAG, "archiveLink: onFailure: " + errorMessage, t);
                showSnackError(errorMessage, false);
            }
        });
    }

    private void closeDrawer() {
        Log.v(LOG_TAG, "closeDrawer()");
        if (mDrawerLayout == null) return;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void getList() {
        Log.v(LOG_TAG, "getList()");
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
                    showSnackError(message, retryGetLinksAction());
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }

                List<Link> downloadedLinks = response.body();
                if (downloadedLinks == null) {
                    String message = "No links returned by server";
                    Log.e(LOG_TAG, "getLinks: onResponse: " + message);
                    showSnackError(message, retryGetLinksAction());
                    return;
                }

                // Store the links in the database
                mLinkStorage.replaceLinks(downloadedLinks);
                mSwipeRefreshLayout.setRefreshing(false);
                if (mLinksAdapter != null) mLinksAdapter.notifyDataSetChanged();
                adapterSetup();
                String message = String.format("Downloaded %d %s", mLinkStorage.getLinksCount(),
                        mLinkStorage.getLinksCount() == 1 ? "link" : "links");
                Log.i(LOG_TAG, "getLinks: onResponse: " + message);
                showSnackSuccess(message);
            }

            @Override
            public void onFailure(Call<List<Link>> call, Throwable t) {
                Log.v(LOG_TAG, "getLinks: onFailure() called with: " + "t = [" + t + "]");
                String errorMessage =
                        "getLinks: onFailure: Error during call: " + t.getLocalizedMessage();
                Log.e(LOG_TAG, errorMessage);
                showSnackError(errorMessage, false);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private Snacks.Action getRefreshSnackAction() {
        return new Snacks.Action(getString(R.string.refresh), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void handleSearch(String query) {
        Log.v(LOG_TAG, "handleSearch() called with: " + "query = [" + query + "]");
        mSearchTerm = query;
        // Keep track of the current filter mode to update the UI if it changes
        int lastFilterMode = mFilterMode;

        if (!mSearchTerm.isEmpty()) {
            Log.v(LOG_TAG, "handleSearch: Searching for mSearchTerm = [" + mSearchTerm + "]");
            if (mFilterMode != LinkStorage.FILTER_SEARCH) {
                // mPreviousFilterMode keeps track of the pre-search filter mode to make restoring
                // that mode easy during onResume(), etc
                mPreviousFilterMode = mFilterMode;
            }
            mFilterMode = LinkStorage.FILTER_SEARCH;
            showSearchResultLinks(mSearchTerm);
        } else {
            Log.v(LOG_TAG, "handleSearch: Restoring previous view state");
            mFilterMode = mPreviousFilterMode;
        }

        if (lastFilterMode != mFilterMode) updateUiAfterFilterModeChange();
    }

    private View.OnClickListener navAccountHideListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNavAccountMenu(false);
                if (mDrawerUsername != null) {
                    mDrawerUsername.setOnClickListener(navAccountShowListener());
                }
            }
        };
    }

    private View.OnClickListener navAccountShowListener() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showNavAccountMenu(true);
                if (mDrawerUsername != null) {
                    mDrawerUsername.setOnClickListener(navAccountHideListener());
                }
            }
        };
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

    private void showAllLinks() {
        mLinksAdapter = new LinksAdapter(this, mLinkStorage.getAllLinks(mFilterMode, mSortMode));
        updateUiAfterAdapterChange();
    }

    private void showCategoryLinks(String category) {
        Log.v(LOG_TAG, "showCategoryLinks() called with: " + "category = [" + category + "]");
        String searchTerm = category;
        if (category.equalsIgnoreCase(getString(R.string.category_uncategorized))) {
            searchTerm = "";
        }
        mLinksAdapter = new LinksAdapter(this, mLinkStorage.findByCategory(searchTerm, mSortMode));
        updateUiAfterAdapterChange();
    }

    private void showNavAccountMenu(final boolean show) {
        Log.v(LOG_TAG, "showNavAccountMenu() called with: " + "show = [" + show + "]");
        if (mNavigationView == null) return;
        Menu menu = mNavigationView.getMenu();
        if (menu == null) return;
        menu.setGroupVisible(R.id.nav_drawer_account, show);
    }

    private void showSearchResultLinks(String searchTerm) {
        Log.v(LOG_TAG,
                "showSearchResultLinks() called with: " + "searchTerm = [" + searchTerm + "]");
        mLinksAdapter = new LinksAdapter(this, mLinkStorage.findByString(searchTerm, mSortMode));
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

    private void updateToolbarTitle() {
        String title = getString(R.string.title_fresh_links);
        String subtitle = getString(R.string.sort_timestamp_descending);
        String format = getString(R.string.title_links_format);

        switch (mFilterMode) {
            case LinkStorage.FILTER_ALL:
                title = getString(R.string.title_all_links);
                break;
            case LinkStorage.FILTER_ARCHIVED:
                title = getString(R.string.title_archived_links);
                break;
            case LinkStorage.FILTER_FAVORITES:
                title = getString(R.string.title_favorite_links);
                break;
            case LinkStorage.FILTER_FRESH:
                title = getString(R.string.title_fresh_links);
                break;
            case LinkStorage.FILTER_CATEGORY:
                format = getString(R.string.title_category_links_format);
                title = mCategory;
                break;
            case LinkStorage.FILTER_SEARCH:
                format = getString(R.string.title_category_links_format);
                title = mSearchTerm;
                break;
        }

        switch (mSortMode) {
            case LinkStorage.SORT_TIMESTAMP_ASCENDING:
                subtitle = getString(R.string.sort_timestamp_ascending);
                break;
            case LinkStorage.SORT_TIMESTAMP_DESCENDING:
                subtitle = getString(R.string.sort_timestamp_descending);
                break;
            case LinkStorage.SORT_TITLE_ASCENDING:
                subtitle = getString(R.string.sort_title_ascending);
                break;
            case LinkStorage.SORT_TITLE_DESCENDING:
                subtitle = getString(R.string.sort_title_descending);
                break;
        }

        title = String.format(format, title, mLinksAdapter.getItemCount());
        Log.i(LOG_TAG, "updateToolbarTitle() called with: "
                + "title = ["
                + title
                + "], subtitle = ["
                + subtitle
                + "], format = ["
                + format
                + "]");
        setToolbarTitle(title, subtitle);
    }

    private void updateUiAfterAdapterChange() {
        if (mLinksAdapter != null) {
            mRecyclerView.setAdapter(mLinksAdapter);
        }
        updateToolbarTitle();
        addLinksChangeListener();
    }

    /**
     * Handles updating the Toolbar title with the current mode the application is in. These modes
     * include which section the user was browsing (All, Archived, etc) and the sorting options they
     * used. This method should be called during onResume(), after a search is cancelled and
     * whenever the user decides to change the current section to browse (All, Archived, etc.)
     */
    private void updateUiAfterFilterModeChange() {
        Log.v(LOG_TAG, "updateUiAfterFilterModeChange()");
        boolean skipShowAllLinks = false;
        switch (mFilterMode) {
            case LinkStorage.FILTER_SEARCH:
                showSearchResultLinks(mSearchTerm);
                break;
            case LinkStorage.FILTER_CATEGORY:
                showCategoryLinks(mCategory);
                skipShowAllLinks = true; // Don't call showAllLinks() after fallthrough
                // Don't call break to handle hiding the SearchView below!
            case LinkStorage.FILTER_ALL:
            case LinkStorage.FILTER_ARCHIVED:
            case LinkStorage.FILTER_FAVORITES:
            case LinkStorage.FILTER_FRESH:
            default:
                // showAllLinks() handles the above filters, keep the fallthrough!
                if (!skipShowAllLinks) showAllLinks();
                mPreviousFilterMode = mFilterMode;
                if (mSearchView != null) mSearchView.onActionViewCollapsed();
                break;
        }
    }

    class BottomSheetLinkListener implements BottomSheetListener {
        private int mLinkPosition;

        @Override
        public void onSheetShown(@NonNull BottomSheet bottomSheet) {
        }

        @Override
        public void onSheetItemSelected(@NonNull BottomSheet bottomSheet, MenuItem menuItem) {
            Log.v(LOG_TAG, "onSheetItemSelected() called with: "
                    + "bottomSheet = ["
                    + bottomSheet
                    + "], menuItem = ["
                    + menuItem
                    + "]");

            int id = menuItem.getItemId();
            switch (id) {
                case (R.id.link_edit):
                    Log.d(LOG_TAG, "onOptionsItemSelected: Link Edit");
                    editLink(mLinkPosition);
                    break;
                case (R.id.link_share):
                    Log.d(LOG_TAG, "onOptionsItemSelected: Link Share");
                    shareLink(mLinkPosition);
                    break;
                case (R.id.link_copy):
                    Log.d(LOG_TAG, "onOptionsItemSelected: Link Copy");
                    copyLink(mLinkPosition);
                    break;
                case (R.id.link_delete):
                    Log.d(LOG_TAG, "onOptionsItemSelected: Link Delete");
                    deleteLink(mLinkPosition);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onSheetDismissed(@NonNull BottomSheet bottomSheet, @DismissEvent int i) {
        }

        BottomSheetLinkListener(int linkPosition) {
            mLinkPosition = linkPosition;
        }
    }
}
