package com.fernandobarillas.linkshare.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.models.ErrorResponse;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.UserInfoResponse;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponseUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.bottomsheet.BottomSheetListener;

import java.util.List;
import java.util.Set;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class LinksListActivity extends BaseLinkActivity
        implements RealmChangeListener<RealmResults<Link>>,
        NavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener {

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

    // Toolbar behavior
    private static final int TOOLBAR_SCROLL_FLAG_RESET = 0;
    private boolean mHideToolbarOnScroll;

    private DrawerLayout       mDrawerLayout;
    private TextView           mDrawerUsername;
    private NavigationView     mNavigationView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView       mRecyclerView;
    private LinksAdapter       mLinksAdapter;
    private SearchView         mSearchView;
    private Toolbar            mToolbar;

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
        Timber.v("onActivityResult() called with: "
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
        } else if (mPreferences.isConfirmExitOnBackPress()) {
            confirmExit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        Timber.v("onResume()");
        super.onResume();
        updateToolbarScrollBehavior();
        if (mLinkStorage.getLinksCount() == 0) {
            getList();
        } else {
            getListIfUpdatedOnServer();
        }
    }

    @Override
    public void onChange(RealmResults<Link> element) {
        Timber.v("onChange() called with: " + "element = [" + element + "]");
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
        Drawable dividerDrawable = ContextCompat.getDrawable(this, R.drawable.link_divider);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(dividerDrawable));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mHideToolbarOnScroll = mPreferences.isHideToolbarOnScroll();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);
            showNavAccountMenu(false);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

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
                Timber.i("onCreate: userdrawer Set drawer text: " + mDrawerUsername.getText());
                mDrawerUsername.setOnClickListener(navAccountShowListener());
            }
        }

        // Show locally stored links right away, if available
        adapterSetup();
    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy()");
        if (mLinksAdapter != null && mLinksAdapter.getLinks() != null) {
            mLinksAdapter.getLinks().removeChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.v("onCreateOptionsMenu() called with: " + "menu = [" + menu + "]");
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
                Timber.v("onCreateOptionsMenu: searchTerm = [" + searchTerm + "]");
                mSearchView.onActionViewExpanded();
                mSearchView.setQuery(searchTerm, false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.v("onOptionsItemSelected() called with: " + "item = [" + item + "]");
        int id = item.getItemId();

        // Keep track of the current SortMode to update the UI if it changes
        int lastSortMode = mSortMode;
        switch (id) {
            case (R.id.sort_title_ascending):
                Timber.d("onOptionsItemSelected: Title Ascending");
                mSortMode = LinkStorage.SORT_TITLE_ASCENDING;
                break;
            case (R.id.sort_title_descending):
                Timber.d("onOptionsItemSelected: Title Descending");
                mSortMode = LinkStorage.SORT_TITLE_DESCENDING;
                break;
            case (R.id.sort_timestamp_ascending):
                Timber.d("onOptionsItemSelected: Timestamp Ascending");
                mSortMode = LinkStorage.SORT_TIMESTAMP_ASCENDING;
                break;
            case (R.id.sort_timestamp_descending):
                Timber.d("onOptionsItemSelected: Timestamp Descending");
                mSortMode = LinkStorage.SORT_TIMESTAMP_DESCENDING;
                break;
            default:
                break;
        }

        if (lastSortMode != mSortMode) {
            performUiUpdate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Timber.v("onNavigationItemSelected() called with: " + "item = [" + item + "]");
        int id = item.getItemId();

        // Keep track of the current filter mode and category to update the UI if they change
        int lastFilterMode = mFilterMode;
        String lastCategory = mCategory;
        if (item.getGroupId() == R.id.nav_drawer_account) {
            Timber.v("onNavigationItemSelected: item = [" + item + "]");
            performLogout();
        } else if (item.getGroupId() == CATEGORIES_MENU_GROUP) {
            mFilterMode = LinkStorage.FILTER_CATEGORY;
            mCategory = item.getTitle().toString();
            Timber.d("onNavigationItemSelected: Tapped category: " + mCategory);
        } else {
            switch (id) {
                case (R.id.nav_fresh_links):
                    Timber.i("onNavigationItemSelected: Displaying fresh Links");
                    mFilterMode = LinkStorage.FILTER_FRESH;
                    break;
                case (R.id.nav_all_links):
                    Timber.i("onNavigationItemSelected: Displaying all Links");
                    mFilterMode = LinkStorage.FILTER_ALL;
                    break;
                case (R.id.nav_favorites):
                    Timber.i("onNavigationItemSelected: Displaying favorite Links");
                    mFilterMode = LinkStorage.FILTER_FAVORITES;
                    break;
                case (R.id.nav_archived):
                    Timber.i("onNavigationItemSelected: Displaying archived Links");
                    mFilterMode = LinkStorage.FILTER_ARCHIVED;
                    break;
                case (R.id.nav_settings):
                    Timber.i("onNavigationItemSelected: Opening Settings");
                    startActivity(new Intent(this, SettingsActivity.class));
                    return false;
                default:
                    break;
            }
        }

        boolean updateUi;
        if (mFilterMode == LinkStorage.FILTER_CATEGORY) {
            // Only update UI if category has changed
            updateUi = mCategory != null && !mCategory.equalsIgnoreCase(lastCategory);
        } else {
            // Only update UI if filter mode has changed
            updateUi = lastFilterMode != mFilterMode;
        }
        if (updateUi) performUiUpdate();
        closeDrawer();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Timber.v("onQueryTextSubmit() called with: " + "query = [" + query + "]");
        handleSearch(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Timber.v("onQueryTextChange() called with: " + "newText = [" + newText + "]");
        handleSearch(newText);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.v("onSaveInstanceState() called with: " + "outState = [" + outState + "]");
        outState.putString(STATE_SEARCH_TERM, mSearchTerm);
        outState.putString(STATE_CATEGORY, mCategory);
        outState.putInt(STATE_FILTER_MODE, mFilterMode);
        outState.putInt(STATE_PREVIOUS_FILTER_MODE, mPreviousFilterMode);
        outState.putInt(STATE_SORT_MODE, mSortMode);
        Timber.d("onSaveInstanceState() returned: " + outState);
        super.onSaveInstanceState(outState);
    }

    public void copyLink(final int position) {
        Timber.v("copyUrl() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Timber.e("copyUrl: Link instance was null before copying URL");
            showSnackError("Could not copy link URL, please refresh", getRefreshSnackAction());
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(CLIPBOARD_LABEL, link.getUrl());
        clipboard.setPrimaryClip(clipData);
        showSnackSuccess("Copied URL: " + link.getUrl());
    }

    public void deleteLink(final int position) {
        Timber.v("deleteLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Timber.e("deleteLink: Link instance was null before making delete API call");
            showSnackError("Could not delete link, please refresh", getRefreshSnackAction());
            return;
        }

        // TODO: maybe add confirmation popup or some UI confirmation element?
        final String title = link.getTitle();
        final String successMessage = "Deleted " + title;
        final String errorMessage = "Error deleting " + title;

        Timber.i("deleteLink: Trying to remove link: " + link);
        Call<Void> deleteCall = mLinkService.deleteLink(link.getLinkId());
        deleteCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Timber.v("deleteLink: onResponse() called with: "
                        + "response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    if (mRecyclerView == null) return;
                    showSnackSuccess(successMessage);
                    if (mLinkStorage != null) mLinkStorage.remove(link);
                    return;
                }

                Timber.e("deleteLink: onResponse: " + errorMessage);
                Timber.e(String.format("deleteLink: onResponse: %d %s",
                        response.code(),
                        response.message()));
                errorResponseHandler(response, errorMessage, position);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Timber.e(t, "deleteLink: onFailure: " + errorMessage);
                errorResponseHandler(null, errorMessage, position);
            }
        });
    }

    public void displayBottomSheet(final int position) {
        Timber.v("displayBottomSheet() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        if (link == null) return;
        new BottomSheet.Builder(this).setSheet(R.menu.link_options)
                .setTitle(link.getTitle())
                .setListener(new BottomSheetLinkMenuListener(position))
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
        Timber.v("openLink() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        Timber.i("openLink: Link: " + link);
        if (link == null) {
            Timber.e("openLink: Null Link when attempting to open URL: " + position);
            showSnackError(getString(R.string.error_link_null), false);
            return;
        }

        String url = link.getUrl();
        if (TextUtils.isEmpty(url)) {
            Timber.e("openLink: Cannot open empty or null link");
            showSnackError(getString(R.string.error_link_url_null), false);
        }
        Timber.i("openLink: Opening URL: " + url);
        openUrlExternally(url);
    }

    public void setFavorite(final int position, final boolean isFavorite) {
        Timber.v("setFavorite() called with: "
                + "position = ["
                + position
                + "], isFavorite = ["
                + isFavorite
                + "]");
        final Link link = mLinksAdapter.getLink(position);
        Timber.i("setFavorite: Link: " + link);
        if (link == null) {
            Timber.e("openLink: Null Link when attempting to favorite position " + position);
            showSnackError(getString(R.string.error_link_favorite), false);
            return;
        }

        // TODO: Extract strings
        final String errorMessage = String.format("API Error %s favorite: %s",
                isFavorite ? "adding" : "removing",
                link.getTitle());

        final Snacks.Action retryAction =
                new Snacks.Action(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Timber.v("setFavorite retryAction onClick() called with: "
                                + "view = ["
                                + view
                                + "]");
                        setFavorite(position, isFavorite);
                    }
                });

        Call<Void> call = mLinkService.favoriteLink(link.getLinkId());
        if (!isFavorite) {
            call = mLinkService.unfavoriteLink(link.getLinkId());
        }

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Timber.v("setFavorite onResponse() called with: "
                        + "call = ["
                        + call
                        + "], response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    Timber.d("setFavorite onResponse: Successful");
                    if (mLinkStorage != null) mLinkStorage.setFavorite(link, isFavorite);
                } else {
                    if (!handleHttpResponseError(response)) {
                        showSnackError(errorMessage, retryAction);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String message = errorMessage;
                if (t != null) {
                    message += " " + t.getLocalizedMessage();
                }
                Timber.e(t, "setFavorite: onFailure: " + message);
                showSnackError(message, retryAction);
            }
        });
    }

    public void shareLink(final int position) {
        Timber.v("shareLink() called with: " + "position = [" + position + "]");
        Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_edit), getRefreshSnackAction());
            return;
        }
        shareUrl(link.getTitle(), link.getUrl());
    }

    public void showLinkCategory(final int position, final boolean isTapAction) {
        Timber.v("showLinkCategory() called with: "
                + "position = ["
                + position
                + "], isTapAction = ["
                + isTapAction
                + "]");
        if (isTapAction && !mPreferences.isTapCategoryToBrowse()) {
            // User has tap category to browse disabled, this is a tap so do nothing
            return;
        }
        Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_show_category), getRefreshSnackAction());
            return;
        }
        String category = link.getCategory();
        if (TextUtils.isEmpty(category)) {
            showSnackError(getString(R.string.error_category_blank), false);
            return;
        }
        mFilterMode = LinkStorage.FILTER_CATEGORY;
        mCategory = category;
        performUiUpdate();
    }

    private void adapterSetup() {
        Timber.v("adapterSetup()");
        if (mLinksAdapter == null) {
            showAllLinks();
        }
        populateDrawerCategories();
        touchHelperSetup();
    }

    private void addLinksChangeListener() {
        if (mLinksAdapter != null && mLinksAdapter.getLinks() != null) {
            RealmResults<Link> links = mLinksAdapter.getLinks();
            links.removeChangeListener(this);
            links.addChangeListener(this);
        }
    }

    private void archiveLink(final int position) {
        Timber.v("archiveLink() called with: " + "position = [" + position + "]");
        final Link link = mLinksAdapter.getLink(position);
        if (link == null) {
            Timber.e("archiveLink: Link instance was null before making archive API call");
            showSnackError("Could not archive link, please refresh", getRefreshSnackAction());
            return;
        }

        final String title = link.getTitle();
        final String successMessage = "Archived " + title;
        final String errorMessage = "Error archiving " + title;

        Timber.i("archiveLink: Trying to archive link: " + link);
        Call<Void> archiveCall = mLinkService.archiveLink(link.getLinkId());
        archiveCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Timber.v("archiveLink: onResponse() called with: "
                        + "response = ["
                        + response
                        + "]");
                if (response.isSuccessful()) {
                    if (mRecyclerView == null) return;
                    showSnackSuccess(successMessage);
                    if (mLinkStorage != null) mLinkStorage.setArchived(link, true);
                    return;
                }

                errorResponseHandler(response, errorMessage, position);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Timber.e(t, "archiveLink: onFailure: " + errorMessage, t);
                errorResponseHandler(null, errorMessage, position);
            }
        });
    }

    private void closeDrawer() {
        Timber.v("closeDrawer()");
        if (mDrawerLayout == null) return;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void confirmExit() {
        Timber.v("confirmExit()");
        DialogInterface.OnClickListener positiveClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.i("confirmExit onClick: User requested exit, finishing Activity");
                        finish();
                    }
                };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_exit_title)
                .setMessage(R.string.confirm_exit_message)
                .setPositiveButton(R.string.confirm_exit_button_positive, positiveClickListener)
                .setNegativeButton(R.string.confirm_exit_button_negative, null)
                .show();
    }

    private void errorResponseHandler(Response<Void> response, String errorMessage, int position) {
        Timber.v("errorResponseHandler() called with: "
                + "response = ["
                + response
                + "], errorMessage = ["
                + errorMessage
                + "]");
        // Restore the item in the UI
        mLinksAdapter.notifyItemChanged(position);
        if (!handleHttpResponseError(response)) {
            showSnackError(errorMessage, false);
        }
    }

    private void getList() {
        Timber.v("getList()");
        mSwipeRefreshLayout.setRefreshing(true);
        Call<List<Link>> call = mLinkService.getLinks();
        call.enqueue(new Callback<List<Link>>() {
            @Override
            public void onResponse(Call<List<Link>> call, Response<List<Link>> response) {
                Timber.v("getLinks: onResponse: " + ResponseUtils.httpCodeString(response));
                mSwipeRefreshLayout.setRefreshing(false);
                if (!response.isSuccessful()) {
                    String message =
                            "Invalid response returned by server: " + ResponseUtils.httpCodeString(
                                    response);
                    Timber.e("getLinks: onResponse: " + message);
                    if (!handleHttpResponseError(response)) {
                        showSnackError(message, retryGetLinksAction());
                    }
                    adapterSetup();
                    return;
                }

                List<Link> downloadedLinks = response.body();
                if (downloadedLinks == null) {
                    String message = "No links returned by server";
                    Timber.e("getLinks: onResponse: " + message);
                    showSnackError(message, retryGetLinksAction());
                    return;
                }

                // Store the links in the database
                mLinkStorage.replaceLinks(downloadedLinks);
                mSwipeRefreshLayout.setRefreshing(false);
                if (mLinksAdapter != null) mLinksAdapter.notifyDataSetChanged();
                adapterSetup();
                String message = String.format("Downloaded %d %s",
                        mLinkStorage.getLinksCount(),
                        mLinkStorage.getLinksCount() == 1 ? "link" : "links");
                Timber.i("getLinks: onResponse: " + message);
                showSnackSuccess(message);
            }

            @Override
            public void onFailure(Call<List<Link>> call, Throwable t) {
                Timber.e("getLinks: onFailure() called with: " + "t = [" + t + "]");
                String errorMessage =
                        "getLinks: onFailure: Error during call: " + t.getLocalizedMessage();
                Timber.e(t, errorMessage);
                showSnackError(errorMessage, false);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void getListIfUpdatedOnServer() {
        Timber.v("getListIfUpdatedOnServer()");
        getUserInfo();
    }

    private Snacks.Action getRefreshSnackAction() {
        return new Snacks.Action(getString(R.string.refresh), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void getUserInfo() {
        Timber.v("getUserInfo()");
        Call<UserInfoResponse> call = mLinkService.getUserInfo();
        final String errorMessage = "Error getting user info";
        call.enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(
                    Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                Timber.v("getUserInfo: onResponse: " + ResponseUtils.httpCodeString(response));
                mSwipeRefreshLayout.setRefreshing(false);
                UserInfoResponse userInfoResponse = response.body();
                if (!response.isSuccessful() || userInfoResponse == null) {
                    Timber.e("getUserInfo: onResponse: Non-successful response from server");

                    if (!handleHttpResponseError(response)) {
                        showSnackError(errorMessage, false);
                    }
                    adapterSetup();
                    return;
                }
                Timber.d("getUserInfo: onResponse: " + userInfoResponse);
                long serverTimestamp = userInfoResponse.getLastUpdateTimestamp();
                long localTimestamp = mPreferences.getLastUpdateTimestamp();
                boolean needsUpdate = localTimestamp < serverTimestamp;
                if (needsUpdate) {
                    Timber.i("onResponse: onResponse: Data on server is newer, updating locally");
                    mPreferences.setLastUpdateTimestamp(userInfoResponse.getLastUpdateTimestamp());
                    getList();
                } else {
                    Timber.i("onResponse: onResponse: Local data is up to date");
                    adapterSetup();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Timber.e(t, "getUserInfo: onFailure: " + errorMessage);
                String errorMessage =
                        "getUserInfo: onFailure: Error during call: " + t.getLocalizedMessage();
                mSwipeRefreshLayout.setRefreshing(false);
                adapterSetup();
                showSnackError(errorMessage, true);
            }
        });
    }

    private boolean handleHttpResponseError(Response response) {
        Timber.v("handleHttpResponseError() called with: " + "response = [" + response + "]");
        if (response == null) return false;
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) return false;

        String format = "Error %d%s";
        String errorMessage = "";
        try {
            ErrorResponse errorResponse =
                    new Gson().fromJson(errorBody.charStream(), ErrorResponse.class);
            if (errorResponse != null) {
                errorMessage = errorResponse.getErrorMessage();
                format = "Error %d: %s";
            }
        } catch (JsonParseException ignored) {
            return false;
        }
        if (TextUtils.isEmpty(errorMessage)) format = "Error %d%s";
        showSnackError(String.format(format, response.code(), errorMessage), true);
        return true;
    }

    private void handleSearch(String query) {
        Timber.v("handleSearch() called with: " + "query = [" + query + "]");
        mSearchTerm = query;
        // Keep track of the current filter mode to update the UI if it changes
        int lastFilterMode = mFilterMode;

        if (!mSearchTerm.isEmpty()) {
            Timber.v("handleSearch: Searching for mSearchTerm = [" + mSearchTerm + "]");
            if (mFilterMode != LinkStorage.FILTER_SEARCH) {
                // mPreviousFilterMode keeps track of the pre-search filter mode to make restoring
                // that mode easy during onResume(), etc
                mPreviousFilterMode = mFilterMode;
            }
            mFilterMode = LinkStorage.FILTER_SEARCH;
            showSearchResultLinks(mSearchTerm);
        } else {
            Timber.v("handleSearch: Restoring previous view state");
            mFilterMode = mPreviousFilterMode;
        }

        if (lastFilterMode != mFilterMode) performUiUpdate();
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

    /**
     * Handles updating the Toolbar title and the links that are displayed with the current mode the
     * application is in. These modes include which section the user was browsing (All, Archived,
     * etc) and the sorting options they used. This method should be called during onResume(), after
     * a search is cancelled and whenever the user decides to change the current section to browse
     * (All, Archived, etc.)
     */
    private void performUiUpdate() {
        Timber.v("performUiUpdate()");
        boolean skipShowAllLinks = false; // Call showAllLinks() after setting the filter mode
        switch (mFilterMode) {
            case LinkStorage.FILTER_SEARCH:
                showSearchResultLinks(mSearchTerm);
                break;
            case LinkStorage.FILTER_CATEGORY:
                showCategoryLinks(mCategory);
                skipShowAllLinks = true; // Don't call showAllLinks(), this is a category change
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

    private void populateDrawerCategories() {
        Timber.v("populateDrawerCategories()");
        if (mLinkStorage == null) return;
        Set<String> categories = mLinkStorage.getCategories();
        if (categories.size() > 0) {
            Menu navMenu = mNavigationView.getMenu();
            navMenu.removeGroup(CATEGORIES_MENU_GROUP); // Avoid duplicate SubMenus
            SubMenu categoriesMenu = navMenu.addSubMenu(CATEGORIES_MENU_GROUP,
                    Menu.NONE,
                    Menu.NONE,
                    getString(R.string.title_categories));
            categoriesMenu.add(CATEGORIES_MENU_GROUP,
                    Menu.NONE,
                    Menu.NONE,
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
        Timber.v("showCategoryLinks() called with: " + "category = [" + category + "]");
        String searchTerm = category;
        if (category.equalsIgnoreCase(getString(R.string.category_uncategorized))) {
            searchTerm = "";
        }
        mLinksAdapter = new LinksAdapter(this, mLinkStorage.findByCategory(searchTerm, mSortMode));
        updateUiAfterAdapterChange();
    }

    private void showNavAccountMenu(final boolean show) {
        Timber.v("showNavAccountMenu() called with: " + "show = [" + show + "]");
        if (mNavigationView == null) return;
        Menu menu = mNavigationView.getMenu();
        if (menu == null) return;
        menu.setGroupVisible(R.id.nav_drawer_account, show);
    }

    private void showSearchResultLinks(String searchTerm) {
        Timber.v("showSearchResultLinks() called with: " + "searchTerm = [" + searchTerm + "]");
        mLinksAdapter = new LinksAdapter(this, mLinkStorage.findByString(searchTerm, mSortMode));
        updateUiAfterAdapterChange();
    }

    private void touchHelperSetup() {
        Timber.v("touchHelperSetup()");
        ItemTouchHelperCallback callback =
                new ItemTouchHelperCallback(new ItemSwipedRightCallback() {
                    @Override
                    public void swipeCallback(RecyclerView.ViewHolder viewHolder) {
                        Timber.v("swipeCallback() called with: "
                                + "viewHolder = ["
                                + viewHolder
                                + "]");
                        archiveLink(viewHolder.getAdapterPosition());
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void updateToolbarScrollBehavior() {
        Timber.v("updateToolbarScrollBehavior()");
        if (mToolbar == null) return;
        AppBarLayout.LayoutParams toolbarParams =
                (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        if (toolbarParams == null) return;

        // Store the current toolbar hide value since the user may have changed it since creation
        boolean oldHideToolbarOnScroll = mHideToolbarOnScroll;
        mHideToolbarOnScroll = mPreferences.isHideToolbarOnScroll();

        toolbarParams.setScrollFlags(TOOLBAR_SCROLL_FLAG_RESET);
        if (mHideToolbarOnScroll) {
            toolbarParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }

        Timber.v("updateToolbarScrollBehavior: toolbar scroll flags = ["
                + toolbarParams.getScrollFlags()
                + "]");

        if (oldHideToolbarOnScroll != mHideToolbarOnScroll) {
            // The user changed the hide preference, recreate the activity to make the new scroll
            // behavior apply
            Timber.d("updateToolbarScrollBehavior: Hide preference changed, recreating");
            recreate();
        }
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
        Timber.i("updateToolbarTitle() called with: "
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

    private class BottomSheetLinkMenuListener implements BottomSheetListener {
        private int mLinkPosition;

        private BottomSheetLinkMenuListener(int linkPosition) {
            mLinkPosition = linkPosition;
        }

        @Override
        public void onSheetShown(@NonNull BottomSheet bottomSheet) {
        }

        @Override
        public void onSheetItemSelected(@NonNull BottomSheet bottomSheet, MenuItem menuItem) {
            Timber.v("onSheetItemSelected() called with: "
                    + "bottomSheet = ["
                    + bottomSheet
                    + "], menuItem = ["
                    + menuItem
                    + "]");

            int id = menuItem.getItemId();
            switch (id) {
                case (R.id.link_edit):
                    Timber.d("onOptionsItemSelected: Link Edit");
                    editLink(mLinkPosition);
                    break;
                case (R.id.link_category):
                    Timber.d("onOptionsItemSelected: Link Category");
                    showLinkCategory(mLinkPosition, false);
                    break;
                case (R.id.link_share):
                    Timber.d("onOptionsItemSelected: Link Share");
                    shareLink(mLinkPosition);
                    break;
                case (R.id.link_copy):
                    Timber.d("onOptionsItemSelected: Link Copy");
                    copyLink(mLinkPosition);
                    break;
                case (R.id.link_delete):
                    Timber.d("onOptionsItemSelected: Link Delete");
                    deleteLink(mLinkPosition);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onSheetDismissed(@NonNull BottomSheet bottomSheet, @DismissEvent int i) {
        }
    }

    // Courtesy of https://www.bignerdranch.com/blog/a-view-divided-adding-dividers-to-your-recyclerview-with-itemdecoration/
    private class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        private DividerItemDecoration(Drawable divider) {
            mDivider = divider;
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params =
                        ((RecyclerView.LayoutParams) child.getLayoutParams());
                int dividerTop = child.getBottom() + params.bottomMargin;
                int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

                mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
                mDivider.draw(canvas);
            }
        }

        @Override
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);

            if (parent.getChildAdapterPosition(view) == 0) {
                // Don't decorate the first child
                return;
            }

            outRect.top = mDivider.getIntrinsicHeight();
        }
    }
}
