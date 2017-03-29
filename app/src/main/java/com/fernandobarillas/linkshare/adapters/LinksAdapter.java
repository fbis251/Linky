package com.fernandobarillas.linkshare.adapters;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fernandobarillas.linkshare.activities.LinksListActivity;
import com.fernandobarillas.linkshare.databinding.ContentLinkBinding;
import com.fernandobarillas.linkshare.models.Link;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import timber.log.Timber;

/**
 * Created by fb on 1/29/16.
 */
public class LinksAdapter extends RealmRecyclerViewAdapter<Link, LinksAdapter.LinkViewHolder> {
    private final LinksListActivity mActivity;

    public LinksAdapter(LinksListActivity activity, @Nullable OrderedRealmCollection<Link> links) {
        super(links, true);
        int linksSize = links == null ? 0 : links.size();
        Timber.v("LinksAdapter() called with: "
                + "activity = ["
                + activity
                + "], links = ["
                + linksSize
                + "]");
        // Link objects have a unique long type ID
        setHasStableIds(true);
        mActivity = activity;
    }

    @Override
    public long getItemId(int index) {
        long result = RecyclerView.NO_ID;
        Link link = getItem(index);
        if (link != null) result = link.getLinkId();
        return result;
    }

    @Override
    public LinkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ContentLinkBinding binding = ContentLinkBinding.inflate(layoutInflater, parent, false);
        return new LinkViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(final LinkViewHolder holder, final int position) {
        final Link link = getItem(position);
        if (link == null) return;
        holder.bind(link);
        // Hide category view (and background drawable) if link has no category set
        boolean isVisible = link.getCategory() != null;
        holder.showCategory(isVisible);
    }

    public class LinkHandler {
        private final LinkViewHolder holder;

        private LinkHandler(LinkViewHolder holder) {
            this.holder = holder;
        }

        public void onClickCategory(final View view) {
            Timber.v("onClickCategory() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.showLinkCategory(getPosition(), true);
        }

        public void onClickFavorite(final View view) {
            Timber.v("onClickFavorite() called with: " + "view = [" + view + "]");
            setFavorite(true);
        }

        public void onClickLayout(View view) {
            Timber.v("onClickLayout() called with: " + "view = [" + view + "]");
            if (mActivity != null) mActivity.openLink(getPosition());
        }

        public void onClickRemoveFavorite(final View view) {
            Timber.v("onClickRemoveFavorite() called with: " + "view = [" + view + "]");
            setFavorite(false);
        }

        public boolean onLongClick(View view) {
            Timber.v("onLongClick() called with: " + "view = [" + view + "]");
            mActivity.displayBottomSheet(getPosition());
            return true;
        }

        private int getPosition() {
            return holder.getAdapterPosition();
        }

        private void setFavorite(final boolean isFavorite) {
            Timber.v("setFavorite() called with: " + "isFavorite = [" + isFavorite + "]");
            if (mActivity != null) mActivity.setLinkFavorite(getPosition(), isFavorite);
        }
    }

    /**
     * ViewHolder that displays individual reddit Links. It uses a CardView in the UI.
     */
    class LinkViewHolder extends RecyclerView.ViewHolder {

        private final ContentLinkBinding mBinding;

        private LinkViewHolder(ContentLinkBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setHandler(new LinkHandler(this));
        }

        private void bind(Link link) {
            mBinding.setLink(link);
            mBinding.executePendingBindings();
        }

        private void showCategory(boolean isVisible) {
            mBinding.linkCategory.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }
}
