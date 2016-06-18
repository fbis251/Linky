package com.fernandobarillas.linkshare.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.databinding.ContentLinkBinding;
import com.fernandobarillas.linkshare.models.Link;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by fb on 1/29/16.
 */

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.LinkViewHolder>
        implements RealmChangeListener<RealmResults<Link>> {
    private static final String LOG_TAG = LinksAdapter.class.getSimpleName();

    Context            mContext;
    LinkStorage        mLinkStorage;
    RealmResults<Link> mLinks;

    Drawable mFavoriteDrawable;
    Drawable mNotFavorite;

    public LinksAdapter(Context context, LinkStorage linkStorage) {
        this(context, linkStorage, null);
    }

    public LinksAdapter(Context context, LinkStorage linkStorage,
            @Nullable RealmResults<Link> links) {
        // TODO: Use RealmResults<Link> passed in here instead of passing in the LinkStorage object
        Log.v(LOG_TAG, "LinksAdapter() called with: "
                + "context = ["
                + context
                + "], linkStorage = ["
                + linkStorage
                + "]");
        mContext = context;
        mLinkStorage = linkStorage;
        mLinks = (links == null) ? mLinkStorage.getAllFreshLinks() : links;
        mLinks.addChangeListener(this);

        // Set up the favorite drawables
        Resources resources = mContext.getResources();
        if (resources == null) return;
        mFavoriteDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite_filled_24dp, null);
        mNotFavorite =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite_border_24dp, null);
    }

    @Override
    public void onChange(RealmResults<Link> element) {
        notifyDataSetChanged();
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

    public void removeChangeListener() {
        Log.v(LOG_TAG, "removeChangeListener()");
        if (mLinks == null) return;
        mLinks.removeChangeListener(this);
    }

    private boolean isPositionValid(int position) {
        return (!mLinks.isEmpty() && position >= 0 && position < mLinks.size());
    }

    private void openLink(final int position) {
        Log.v(LOG_TAG, "openLink() called with: " + "position = [" + position + "]");
        if (mContext == null) {
            Log.e(LOG_TAG, "openLink: Invalid context, cancelling opening link");
            return;
        }

        if (getLink(position) == null) {
            Log.e(LOG_TAG, "openLink: Null Link when attempting to open URL: " + position);
            return;
        }

        String url = getLink(position).getUrl();
        if (TextUtils.isEmpty(url)) {
            Log.e(LOG_TAG, "openLink: Cannot open empty or null link");
            // TODO: Show UI error
        }
        Log.i(LOG_TAG, "openLink: Opening URL: " + url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make sure that we have applications installed that can handle this intent
        if (browserIntent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(browserIntent);
        } else {
            // TODO: Show UI error
        }
    }

    public class LinkHandler {
        private LinkViewHolder holder;

        public LinkHandler(LinkViewHolder holder) {
            this.holder = holder;
        }

        public void onClickFavorite(View view) {
            Log.v(LOG_TAG, "onClickFavorite() called with: " + "view = [" + view + "]");
            if (view == null || mFavoriteDrawable == null) return;
            if (!(view instanceof ImageView)) return;
            ImageView imageView = (ImageView) view;
            imageView.setImageDrawable(mFavoriteDrawable);
            setFavorite(true);
        }

        public void onClickNotFavorite(View view) {
            Log.v(LOG_TAG, "onClickNotFavorite() called with: " + "view = [" + view + "]");
            if (view == null || mNotFavorite == null) return;
            if (!(view instanceof ImageView)) return;
            ImageView imageView = (ImageView) view;
            imageView.setImageDrawable(mNotFavorite);
            setFavorite(false);
        }

        public void onClickTitle(View view) {
            Log.v(LOG_TAG, "onClickTitle() called with: " + "view = [" + view + "]");
            openLink(getHolderPosition());
        }

        private int getHolderPosition() {
            return holder.getLayoutPosition();
        }

        private void setFavorite(boolean isFavorite) {
            Log.v(LOG_TAG, "setFavorite() called with: " + "isFavorite = [" + isFavorite + "]");
            Log.i(LOG_TAG, "setFavorite: Position: " + getHolderPosition());
            Link link = getLink(getHolderPosition());
            if (link == null) return;
            mLinkStorage.setFavorite(link, isFavorite);
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
            mBinding.setHandlers(new LinkHandler(this));
        }

        public ContentLinkBinding getBinding() {
            return mBinding;
        }
    }
}
