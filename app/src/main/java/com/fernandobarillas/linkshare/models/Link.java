package com.fernandobarillas.linkshare.models;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.realm.RealmModel;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by fb on 1/28/16.
 */
@RealmClass
public class Link implements RealmModel {

    public static final int TITLE_MAX_LENGTH    = 100;
    public static final int CATEGORY_MAX_LENGTH = 50;

    @PrimaryKey
    private long    linkId;
    @Index
    private String  category;
    private boolean isArchived;
    private boolean isFavorite;
    private long    timestamp;
    private String  title;
    private String  url;

    public Link() {
    }

    public Link(
            long linkId,
            String category,
            boolean isArchived,
            boolean isFavorite,
            long timestamp,
            String title,
            String url) {
        this.linkId = linkId;
        this.timestamp = timestamp;
        setCategory(category);
        setArchived(isArchived);
        setFavorite(isFavorite);
        setTitle(title);
        setUrl(url);
    }

    public Link(int linkId, String category, int timestamp, String title, String url) {
        this.linkId = linkId;
        this.timestamp = timestamp;
        setCategory(category);
        setTitle(title);
        setUrl(url);
    }

    public Link(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Link{"
                + "linkId="
                + linkId
                + ", category='"
                + category
                + '\''
                + ", isArchived="
                + isArchived
                + ", isFavorite="
                + isFavorite
                + ", timestamp="
                + timestamp
                + ", title='"
                + title
                + '\''
                + ", url='"
                + url
                + '\''
                + '}';
    }

    @Nullable
    public String getCategory() {
        return TextUtils.isEmpty(category) ? null : category;
    }

    public void setCategory(String category) {
        this.category = category;
        if (this.category == null) return;
        // TODO: 9/30/16 Consider adding ellipsis to indicate truncation
        this.category = category.trim()
                .toLowerCase()
                .substring(0, Math.min(category.length(), CATEGORY_MAX_LENGTH));
    }

    public String getDomain() {
        if (url == null) return null;
        try {
            URL urlObject = new URL(url);
            return urlObject.getHost();
        } catch (MalformedURLException ignored) {
        }

        return null;
    }

    public long getLinkId() {
        return linkId;
    }

    public String getTimeString() {
        if (timestamp < 1) return null;
        long milliseconds = timestamp * 1000;

        return DateUtils.getRelativeTimeSpanString(milliseconds,
                new Date().getTime(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date date) {
        this.timestamp = TimeUnit.MILLISECONDS.toSeconds(date.getTime());
    }

    public String getTitle() {
        return !TextUtils.isEmpty(title) ? title : getDomain();
    }

    public void setTitle(String title) {
        this.title = title;
        if (this.title == null) return;
        // TODO: 9/30/16 Consider adding ellipsis to indicate truncation
        this.title = title.trim().substring(0, Math.min(title.length(), TITLE_MAX_LENGTH));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
