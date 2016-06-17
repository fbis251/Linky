package com.fernandobarillas.linkshare.adapters;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.databinding.library.baseAdapters.BR;
import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databases.LinkStorage;
import com.fernandobarillas.linkshare.databinding.ContentLinkBinding;
import com.fernandobarillas.linkshare.models.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fb on 1/29/16.
 */

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.LinkViewHolder> {
    private static final String LOG_TAG = LinksAdapter.class.getSimpleName();

    Context     mContext;
    LinkStorage mLinkStorage;
    List<Link> mLinksList = new ArrayList<>();

    public LinksAdapter(Context context, LinkStorage linkStorage) {
        Log.v(LOG_TAG, "LinksAdapter() called with: "
                + "context = ["
                + context
                + "], linkStorage = ["
                + linkStorage
                + "]");
        mContext = context;
        mLinkStorage = linkStorage;
        mLinksList = mLinkStorage.getAllLinks();
    }

    @Override
    public LinkViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.content_link, viewGroup, false);
        return new LinkViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final LinkViewHolder holder, final int linkId) {
        final Link link = mLinksList.get(linkId);
        holder.getBinding().setVariable(BR.link, link);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mLinksList.size();
    }

    public Link getLink(int position) {
        if (mLinksList.isEmpty() || position < 0 || position > mLinksList.size()) {
            return null;
        }

        return mLinksList.get(position);
    }

    public Link remove(int linkId) {
        if (mLinksList.isEmpty() || linkId < 0 || linkId >= mLinksList.size()) {
            return null;
        }

        Link removedLink = mLinksList.remove(linkId);
        Link resultLink = new Link(removedLink);
        mLinkStorage.remove(removedLink); // Delete from database
        return resultLink;
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

    /**
     * ViewHolder that displays individual reddit Links. It uses a CardView in the UI.
     */
    public class LinkViewHolder extends RecyclerView.ViewHolder {

        protected ContentLinkBinding mBinding;

        public LinkViewHolder(View view) {
            super(view);
            mBinding = DataBindingUtil.bind(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLink(getLayoutPosition());
                }
            });
        }

        public ContentLinkBinding getBinding() {
            return mBinding;
        }
    }
}
