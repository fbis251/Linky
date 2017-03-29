package com.fernandobarillas.linkshare.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.adapters.LinksAdapter;
import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.models.ErrorResponse;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.UserInfoResponse;
import com.fernandobarillas.linkshare.ui.CategoryDrawerItem;
import com.fernandobarillas.linkshare.ui.ItemTouchHelperCallback;
import com.fernandobarillas.linkshare.ui.Snacks;
import com.fernandobarillas.linkshare.utils.ResponseUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.bottomsheet.BottomSheetListener;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
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
        implements RealmChangeListener<RealmResults<Link>>, SearchView.OnQueryTextListener,
        AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {

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

    // Link Update actions
    private static final int UPDATE_ARCHIVE  = 0;
    private static final int UPDATE_DELETE   = 1;
    private static final int UPDATE_FAVORITE = 2;

    // Toolbar behavior
    private static final int TOOLBAR_SCROLL_FLAG_RESET = 0;

    // Drawer Navigation
    private static final int DRAWER_NAV_LOG_OUT   = -2;
    private static final int DRAWER_NAV_PROFILE   = -1;
    private static final int DRAWER_NAV_FRESH     = 0;
    private static final int DRAWER_NAV_ALL       = 1;
    private static final int DRAWER_NAV_FAVORITES = 2;
    private static final int DRAWER_NAV_ARCHIVED  = 3;
    private static final int DRAWER_NAV_SETTINGS  = 4;

    // Links to display
    private RealmResults<Link> mLinks;

    // UI Behavior
    private boolean mHideToolbarOnScroll;

    private Drawer               mDrawer;
    private SwipeRefreshLayout   mSwipeRefreshLayout;
    private RecyclerView         mRecyclerView;
    private LinksAdapter         mLinksAdapter;
    private SearchView           mSearchView;
    private Toolbar              mToolbar;
    private ExpandableDrawerItem mCategoriesDrawerItem;

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
        if (mDrawer != null && mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
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
        updateDrawerFreshLinkCount();
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
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.links_swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.links_recycler_view);
        Drawable dividerDrawable = ContextCompat.getDrawable(this, R.drawable.link_divider);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(dividerDrawable));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mHideToolbarOnScroll = mPreferences.isHideToolbarOnScroll();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getList();
            }
        });

        // Set up the navigation drawer
        drawerSetup(savedInstanceState);
        // Show locally stored links right away, if available
        adapterSetup();
    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy()");
        if (mLinks != null) mLinks.removeChangeListener(this);
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
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        Timber.v("onItemClick() called with: "
                + "view = ["
                + view
                + "], position = ["
                + position
                + "], drawerItem = ["
                + drawerItem
                + "]");
        // Keep track of the current filter mode and category to update the UI if they change
        int lastFilterMode = mFilterMode;
        String lastCategory = mCategory;

        if (drawerItem instanceof CategoryDrawerItem) {
            CategoryDrawerItem categoryDrawerItem = (CategoryDrawerItem) drawerItem;
            mFilterMode = LinkStorage.FILTER_CATEGORY;
            mCategory = categoryDrawerItem.getName().getText(this);
            Timber.d("onItemClick: Tapped category: " + mCategory);
        } else {
            // Cast to int since the IDs we've set for each item are all integer constants
            int itemId = (int) drawerItem.getIdentifier();
            switch (itemId) {
                case (DRAWER_NAV_FRESH):
                    Timber.i("onItemClick: Displaying fresh Links");
                    mFilterMode = LinkStorage.FILTER_FRESH;
                    break;
                case (DRAWER_NAV_ALL):
                    Timber.i("onItemClick: Displaying all Links");
                    mFilterMode = LinkStorage.FILTER_ALL;
                    break;
                case (DRAWER_NAV_FAVORITES):
                    Timber.i("onItemClick: Displaying favorite Links");
                    mFilterMode = LinkStorage.FILTER_FAVORITES;
                    break;
                case (DRAWER_NAV_ARCHIVED):
                    Timber.i("onItemClick: Displaying archived Links");
                    mFilterMode = LinkStorage.FILTER_ARCHIVED;
                    break;
                case (DRAWER_NAV_SETTINGS):
                    Timber.i("onItemClick: Opening Settings");
                    openSettings();
                    return true; // Don't close nav drawer, don't need to do the UI update below
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
        return false; // Returning false will close the drawer after click
    }

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        Timber.v("onProfileChanged() called with: "
                + "view = ["
                + view
                + "], profile = ["
                + profile
                + "], current = ["
                + current
                + "]");
        if (profile instanceof IDrawerItem && profile.getIdentifier() == DRAWER_NAV_LOG_OUT) {
            Timber.i("onProfileChanged: Performing logout");
            performLogout();
        }
        return false;
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

    public void displayBottomSheet(final int position) {
        Timber.v("displayBottomSheet() called with: " + "position = [" + position + "]");
        Link link = getLink(position);
        if (link == null) return;
        new BottomSheet.Builder(this).setSheet(R.menu.link_options)
                .setTitle(link.getTitle())
                .setListener(new BottomSheetLinkMenuListener(position))
                .show();
    }

    public void openLink(final int position) {
        Timber.v("openLink() called with: " + "position = [" + position + "]");
        Link link = getLink(position);
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

    public void setLinkFavorite(final int position, final boolean isFavorite) {
        Timber.v("setLinkFavorite() called with: "
                + "position = ["
                + position
                + "], isFavorite = ["
                + isFavorite
                + "]");
        final Link link = getLink(position);
        if (link == null) {
            Timber.e("openLink: Null Link when attempting to favorite position " + position);
            showSnackError(getString(R.string.error_link_favorite), false);
            return;
        }
        Call<Void> favoriteCall = isFavorite ? mLinkService.favoriteLink(link.getLinkId())
                : mLinkService.unfavoriteLink(link.getLinkId());
        favoriteCall.enqueue(new LinkUpdateCallback(link, UPDATE_FAVORITE, isFavorite, position));
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
        Link link = getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_show_category), getRefreshSnackAction());
            return;
        }
        String category = link.getCategory();
        if (TextUtils.isEmpty(category)) {
            category = getString(R.string.category_uncategorized);
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
        updateDrawerFreshLinkCount();
        populateDrawerCategories();
        touchHelperSetup();
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

    private void copyLink(final int position) {
        Timber.v("copyUrl() called with: " + "position = [" + position + "]");
        final Link link = getLink(position);
        if (link == null) {
            Timber.e("copyUrl: Link instance was null before copying URL");
            showSnackError(getString(R.string.link_update_error_refresh), getRefreshSnackAction());
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(CLIPBOARD_LABEL, link.getUrl());
        clipboard.setPrimaryClip(clipData);
        showSnackSuccess("Copied URL: " + link.getUrl());
    }

    private void deleteLink(final int position) {
        Timber.v("deleteLink() called with: " + "position = [" + position + "]");
        final Link link = getLink(position);
        if (link == null) {
            Timber.e("deleteLink: Link instance was null before making delete API call");
            showSnackError(getString(R.string.link_update_error_refresh), getRefreshSnackAction());
            return;
        }
        Call<Void> deleteCall = mLinkService.deleteLink(link.getLinkId());
        deleteCall.enqueue(new LinkUpdateCallback(link, UPDATE_DELETE, false, position));
    }

    private void drawerSetup(final Bundle savedInstanceState) {
        Timber.v("drawerSetup() called with: "
                + "savedInstanceState = ["
                + savedInstanceState
                + "]");
        if (mPreferences == null || mLinkStorage == null) return;
        String username = mPreferences.getUsername();
        if (username == null) username = "";

        String domain = "";
        if (mLinksApi != null) domain = mLinksApi.getApiUrlWithScheme();
        final IProfile profile = new ProfileDrawerItem().withNameShown(true)
                .withName(username)
                .withEmail(domain)
                .withIcon(getUserProfileDrawable(username))
                .withIdentifier(DRAWER_NAV_PROFILE);

        AccountHeader accountHeader = new AccountHeaderBuilder().withActivity(this)
                .withCompactStyle(true)
                .withHeaderBackground(R.drawable.drawer_header_background)
                .addProfiles(profile,
                        new ProfileSettingDrawerItem().withName(getString(R.string.nav_log_out))
                                .withDescription(getString(R.string.nav_log_out_description))
                                .withIcon(R.drawable.ic_log_out_black_24dp)
                                .withIdentifier(DRAWER_NAV_LOG_OUT))
                .withOnAccountHeaderListener(this)
                .withSavedInstance(savedInstanceState)
                .build();

        mCategoriesDrawerItem = new ExpandableDrawerItem().withName(R.string.title_categories);

        mDrawer = new DrawerBuilder().withActivity(this)
                .withTranslucentStatusBar(false)
                .withAccountHeader(accountHeader)
                .withHasStableIds(true)
                .withToolbar(mToolbar)
                .addDrawerItems(new PrimaryDrawerItem().withName(R.string.nav_fresh_links)
                                .withIcon(R.drawable.ic_fresh_links_24dp)
                                .withIdentifier(DRAWER_NAV_FRESH)
                                .withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE)
                                        .withColorRes(R.color.md_red_700)),
                        new PrimaryDrawerItem().withName(R.string.nav_all_links)
                                .withIcon(R.drawable.ic_all_links_24dp)
                                .withIdentifier(DRAWER_NAV_ALL),
                        new PrimaryDrawerItem().withName(R.string.nav_favorite_links)
                                .withIcon(R.drawable.ic_favorite_filled_black_24dp)
                                .withIdentifier(DRAWER_NAV_FAVORITES),
                        new PrimaryDrawerItem().withName(R.string.nav_archived_links)
                                .withIcon(R.drawable.ic_archived_24dp)
                                .withIdentifier(DRAWER_NAV_ARCHIVED),
                        new PrimaryDrawerItem().withName(R.string.nav_settings)
                                .withIcon(R.drawable.ic_settings_24dp)
                                .withIdentifier(DRAWER_NAV_SETTINGS),
                        new DividerDrawerItem(),
                        mCategoriesDrawerItem)
                .withSavedInstance(savedInstanceState)
                .withOnDrawerItemClickListener(this)
                .build();
    }

    private void editLink(final int position) {
        Link link = getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_edit), getRefreshSnackAction());
            return;
        }
        Intent editIntent = new Intent(getApplicationContext(), EditLinkActivity.class);
        editIntent.putExtra(EditLinkActivity.EXTRA_LINK_ID, link.getLinkId());
        startActivityForResult(editIntent, EDIT_LINK_REQUEST);
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

    private Link getLink(final int position) {
        if (mLinks == null) return null;
        try {
            return mLinks.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            Timber.e(e, "getLink: Invalid array index: %d", position);
            return null;
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

    private Drawable getUserProfileDrawable(String username) {
        if (username == null) username = "";
        String firstLetter = username.length() > 0 ? username.substring(0, 1) : "";
        int backgroundColor = ContextCompat.getColor(this, R.color.drawerUserIconBackground);
        int textColor = ContextCompat.getColor(this, R.color.drawerUserIconText);
        int iconSizeDimen = R.dimen.material_drawer_item_profile_icon;
        int iconSize = getResources().getDimensionPixelSize(iconSizeDimen);
        return TextDrawable.builder()
                .beginConfig()
                .textColor(textColor)
                .toUpperCase()
                .width(iconSize)
                .height(iconSize)
                .endConfig()
                .buildRect(firstLetter, backgroundColor);
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
        if (mDrawer == null || mCategoriesDrawerItem == null || mLinkStorage == null) return;
        List<IDrawerItem> drawerCategoriesList = new ArrayList<>();
        drawerCategoriesList.add(new CategoryDrawerItem(R.string.category_uncategorized));
        Set<String> categoriesSet = mLinkStorage.getCategories();
        for (String category : categoriesSet) {
            drawerCategoriesList.add(new CategoryDrawerItem(category));
        }

        // Update the DrawerItem with the new List of categories
        mCategoriesDrawerItem.withSubItems(drawerCategoriesList);
        mDrawer.updateItem(mCategoriesDrawerItem);
        Timber.v("populateDrawerCategories: category count = ["
                + drawerCategoriesList.size()
                + "]");
    }

    private Snacks.Action retryGetLinksAction() {
        return new Snacks.Action(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });
    }

    private void setLinkArchived(final int position, final boolean isArchived) {
        Timber.v("setLinkArchived() called with: "
                + "position = ["
                + position
                + "], isArchived = ["
                + isArchived
                + "]");
        final Link link = getLink(position);
        if (link == null) {
            Timber.e("setLinkArchived: Link instance was null before making archive API call");
            showSnackError(getString(R.string.link_update_error_refresh), getRefreshSnackAction());
            return;
        }
        Call<Void> archiveCall = isArchived ? mLinkService.archiveLink(link.getLinkId())
                : mLinkService.unarchiveLink(link.getLinkId());
        archiveCall.enqueue(new LinkUpdateCallback(link, UPDATE_ARCHIVE, isArchived, position));
    }

    private void shareLink(final int position) {
        Timber.v("shareLink() called with: " + "position = [" + position + "]");
        Link link = getLink(position);
        if (link == null) {
            showSnackError(getString(R.string.error_cannot_edit), getRefreshSnackAction());
            return;
        }
        shareUrl(link.getTitle(), link.getUrl());
    }

    private void showAllLinks() {
        mLinks = mLinkStorage.getAllLinks(mFilterMode, mSortMode);
        mLinksAdapter = new LinksAdapter(this, mLinks);
        updateUiAfterAdapterChange();
    }

    private void showCategoryLinks(String category) {
        Timber.v("showCategoryLinks() called with: " + "category = [" + category + "]");
        String searchTerm = category;
        if (category.equalsIgnoreCase(getString(R.string.category_uncategorized))) {
            searchTerm = "";
        }
        mLinks = mLinkStorage.findByCategory(searchTerm, mSortMode);
        mLinksAdapter = new LinksAdapter(this, mLinks);
        updateUiAfterAdapterChange();
    }

    private void showSearchResultLinks(String searchTerm) {
        Timber.v("showSearchResultLinks() called with: " + "searchTerm = [" + searchTerm + "]");
        mLinks = mLinkStorage.findByString(searchTerm, mSortMode);
        mLinksAdapter = new LinksAdapter(this, mLinks);
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
                        setLinkArchived(viewHolder.getAdapterPosition(), true);
                    }
                });
        ItemTouchHelper simpleItemTouchHelper = new ItemTouchHelper(callback);
        simpleItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void updateDrawerFreshLinkCount() {
        Timber.v("updateDrawerFreshLinkCount() called");
        if (mDrawer == null || mLinkStorage == null) return;
        long freshLinkCount = mLinkStorage.getFreshLinkCount();
        String newCount = Long.toString(freshLinkCount);
        mDrawer.updateBadge(DRAWER_NAV_FRESH, new StringHolder(newCount));
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
        if (mLinks != null) {
            mLinks.removeChangeListener(this);
            mLinks.addChangeListener(this);
        }
    }

    // Link Update actions
    @IntDef({UPDATE_ARCHIVE, UPDATE_DELETE, UPDATE_FAVORITE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface UpdateType {
    }

    private class BottomSheetLinkMenuListener implements BottomSheetListener {
        private final int mLinkPosition;

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
        private final Drawable mDivider;

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

    private class LinkUpdateCallback implements Callback<Void> {
        private final Link    mLink;
        @UpdateType
        private final int     mUpdateType;
        private final boolean mNewIsArchivedOrFavoriteValue;
        private final String  mSuccessMessage;
        private final int     mPosition;

        private String mErrorMessage;
        private String mUpdateAction;

        private LinkUpdateCallback(
                final Link link,
                final @UpdateType int updateType,
                final boolean newIsArchivedOrFavoriteValue,
                final int position) {
            mLink = link;
            mUpdateType = updateType;
            mNewIsArchivedOrFavoriteValue = newIsArchivedOrFavoriteValue;
            mPosition = position;

            mUpdateAction = "";
            switch (mUpdateType) {
                case UPDATE_ARCHIVE:
                    mUpdateAction = mNewIsArchivedOrFavoriteValue ? getString(R.string.archive)
                            : getString(R.string.unarchive);
                    break;
                case UPDATE_DELETE:
                    mUpdateAction = getString(R.string.delete);
                    break;
                case UPDATE_FAVORITE:
                    mUpdateAction = mNewIsArchivedOrFavoriteValue ? getString(R.string.favorite)
                            : getString(R.string.unfavorite);
                    break;
            }

            String title = link != null ? link.getTitle() : "";
            mSuccessMessage = getString(R.string.link_update_success, mUpdateAction, title);
            mErrorMessage = getString(R.string.link_update_error, mUpdateAction, title);

            Timber.i("LinkUpdateCallback: Trying to %s: %s", mUpdateAction, title);
        }

        @Override
        public void onResponse(Call<Void> call, Response<Void> response) {
            Timber.v(mUpdateAction
                    + " onResponse() called with: "
                    + "call = ["
                    + call
                    + "], response = ["
                    + response
                    + "]");
            if (response.isSuccessful()) {
                if (mLinkStorage == null) return;
                switch (mUpdateType) {
                    case UPDATE_ARCHIVE:
                        mLinkStorage.setArchived(mLink, mNewIsArchivedOrFavoriteValue);
                        break;
                    case UPDATE_DELETE:
                        mLinkStorage.remove(mLink);
                        break;
                    case UPDATE_FAVORITE:
                        mLinkStorage.setFavorite(mLink, mNewIsArchivedOrFavoriteValue);
                        break;
                }
                showSnackSuccess(mSuccessMessage);
            } else {
                String logMessage = String.format("%s onResponse: %d %s",
                        mUpdateAction,
                        response.code(),
                        response.message());
                Timber.e(logMessage);
                errorResponseHandler(response, mErrorMessage, mPosition);
            }
        }

        @Override
        public void onFailure(Call<Void> call, Throwable t) {
            Timber.e(t, "%s onFailure", mUpdateAction);
            if (t != null) {
                mErrorMessage = getString(R.string.link_update_error,
                        mUpdateAction,
                        t.getLocalizedMessage());
            }
            errorResponseHandler(null, mErrorMessage, mPosition);
        }
    }
}
