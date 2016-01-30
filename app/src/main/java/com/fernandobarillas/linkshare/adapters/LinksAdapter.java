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

import java.util.List;

/**
 * Created by fb on 1/29/16.
 */

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.LinkViewHolder> {
    private static final String LOG_TAG = LinksAdapter.class.getSimpleName();
    Context mContext;
    List<String> mLinksList;

    public LinksAdapter(Context context, List<String> linksList) {
        mContext = context;
        mLinksList = linksList;
    }

    @Override
    public LinkViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView =
                LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_link, viewGroup, false);
        return new LinkViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final LinkViewHolder linkViewHolder, final int linkId) {
        linkViewHolder.linkId = linkId;
        final String url = mLinksList.get(linkId);
        // Set the strings in the card
        linkViewHolder.mLinkTitle.setText(url);
        linkViewHolder.mLinkTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLink(url);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mLinksList.size();
    }

    private void openLink(String url) {
        Log.v(LOG_TAG, "openLink() called with: " + "url = [" + url + "]");
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
    public static class LinkViewHolder extends RecyclerView.ViewHolder {

        protected int linkId;

        protected TextView mLinkTitle;

        public LinkViewHolder(View view) {
            super(view);
            mLinkTitle = (TextView) view.findViewById(R.id.link_url);
        }
    }
}
