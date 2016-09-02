package com.fernandobarillas.linkshare.adapters;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.databinding.library.baseAdapters.BR;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.activities.LinksListActivity;
import com.fernandobarillas.linkshare.databinding.ContentLinkBinding;
import com.fernandobarillas.linkshare.models.Link;

import io.realm.RealmResults;

/**
 * Created by fb on 1/29/16.
 */

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.LinkViewHolder> {
    private static final String LOG_TAG = LinksAdapter.class.getSimpleName();

    LinksListActivity  mActivity;
    RealmResults<Link> mLinks;

    Drawable mFavoriteDrawable;
    Drawable mNotFavorite;

    public LinksAdapter(LinksListActivity activity, @NonNull RealmResults<Link> links) {
        Log.v(LOG_TAG, "LinksAdapter() called with: "
                + "activity = ["
                + activity
                + "], links = ["
                + links
                + "]");
        mActivity = activity;
        mLinks = links;

        // Set up the favorite drawables
        Resources resources = mActivity.getResources();
        if (resources == null) return;
        mFavoriteDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite_filled_24dp, null);
        mNotFavorite =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite_border_24dp, null);
    }

    @Override
    public LinkViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.content_link, viewGroup, false);
        return new LinkViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final LinkViewHolder holder, final int position) {
        final Link link = mLinks.get(position);
        holder.getBinding().setVariable(BR.link, link);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mLinks.size();
    }

    public Link getLink(int position) {
        if (!isPositionValid(position)) {
            return null;
        }

        return mLinks.get(position);
    }

    public RealmResults<Link> getLinks() {
        return mLinks;
    }

    private boolean isPositionValid(int position) {
        return (!mLinks.isEmpty() && position >= 0 && position < mLinks.size());
    }

    public class LinkHandler {
        private LinkViewHolder holder;
        private LinearLayout   linkTools;

        public LinkHandler(LinkViewHolder holder) {
            this.holder = holder;
            linkTools = holder.getBinding().linkToolsLayout;
        }

        public void onClickDelete(View view) {
            Log.v(LOG_TAG, "onClickDelete() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.deleteLink(getPosition());
            showTools(false);
        }

        public void onClickEdit(View view) {
            Log.v(LOG_TAG, "onClickEdit() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.editLink(getPosition());
            showTools(false);
        }

        public void onClickFavorite(final View view) {
            Log.v(LOG_TAG, "onClickFavorite() called with: " + "view = [" + view + "]");
            setFavorite(true);
            showTools(false);
        }

        public void onClickLayout(View view) {
            Log.v(LOG_TAG, "onClickLayout() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.openLink(getPosition());
        }

        public void onClickRemoveFavorite(final View view) {
            Log.v(LOG_TAG, "onClickRemoveFavorite() called with: " + "view = [" + view + "]");
            setFavorite(false);
            showTools(false);
        }

        public void onClickShare(View view) {
            Log.v(LOG_TAG, "onClickShare() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.shareLink(getPosition());
            showTools(false);
        }

        public boolean onLongClick(View view) {
            Log.v(LOG_TAG, "onLongClick() called with: " + "view = [" + view + "]");
            // TODO: Show link BottomSheet for sharing, edit, delete, copy text etc
            toggleTools();
            return true;
        }

        private int getPosition() {
            return holder.getAdapterPosition();
        }

        private void setFavorite(final boolean isFavorite) {
            Log.v(LOG_TAG, "setFavorite() called with: " + "isFavorite = [" + isFavorite + "]");
            if (mActivity != null) mActivity.setFavorite(getPosition(), isFavorite);
        }

        private void showTools(final boolean show) {
            Log.v(LOG_TAG, "showTools() called with: " + "show = [" + show + "]");
            if (linkTools == null) return;
            // TODO: Add animation to showing and hiding
            linkTools.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        private void toggleTools() {
            Log.v(LOG_TAG, "toggleTools()");
            if (linkTools == null) {
                Log.e(LOG_TAG, "toggleTools: Tools was null!");
                return;
            }
            showTools(linkTools.getVisibility() != View.VISIBLE);
        }
    }

    /**
     * ViewHolder that displays individual reddit Links. It uses a CardView in the UI.
     */
    public class LinkViewHolder extends RecyclerView.ViewHolder {

        protected ContentLinkBinding mBinding;

        public LinkViewHolder(View view) {
            super(view);
            mBinding = DataBindingUtil.bind(view);
            mBinding.setHandler(new LinkHandler(this));
        }

        public ContentLinkBinding getBinding() {
            return mBinding;
        }
    }
}
