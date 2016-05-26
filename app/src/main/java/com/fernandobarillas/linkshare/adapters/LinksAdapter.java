package com.fernandobarillas.linkshare.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fernandobarillas.linkshare.R;
import com.fernandobarillas.linkshare.databases.LinkStorage;
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

    @Override public LinkViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.content_link, viewGroup, false);
        return new LinkViewHolder(itemView);
    }

    @Override public void onBindViewHolder(final LinkViewHolder linkViewHolder, final int linkId) {
        final Link link = mLinksList.get(linkId);
        // Set the strings in the card
        linkViewHolder.mLinkTitle.setText(link.getTitle());
        linkViewHolder.mLinkUrl.setText(link.getUrl());
    }

    @Override public int getItemCount() {
        return mLinksList.size();
    }

    public String getUrl(int linkId) {
        if (mLinksList.isEmpty() || linkId < 0 || linkId > mLinksList.size()) {
            return null;
        }

        Log.e(LOG_TAG, "getUrl: Database ID for link: " + mLinksList.get(linkId));

        return mLinksList.get(linkId).getUrl();
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

    private void openLink(final int linkId) {
        Log.v(LOG_TAG, "openLink() called with: " + "linkId = [" + linkId + "]");
        String url = getUrl(linkId);
        if (mContext == null) {
            return;
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make sure that we have applications installed that can handle this intent
        if (browserIntent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(browserIntent);
        }
    }

    /**
     * ViewHolder that displays individual reddit Links. It uses a CardView in the UI.
     */
    public class LinkViewHolder extends RecyclerView.ViewHolder {

        protected TextView mLinkTitle;

        protected TextView mLinkUrl;

        public LinkViewHolder(View view) {
            super(view);
            mLinkTitle = (TextView) view.findViewById(R.id.link_title);
            mLinkUrl = (TextView) view.findViewById(R.id.link_url);

            view.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    openLink(getLayoutPosition());
                }
            });
        }
    }
}
