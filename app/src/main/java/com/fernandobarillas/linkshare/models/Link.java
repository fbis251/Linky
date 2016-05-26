package com.fernandobarillas.linkshare.models;


import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;

import io.realm.RealmObject;

/**
 * Created by fb on 1/28/16.
 */
public class Link extends RealmObject {
    int linkId;
    String category = "";
    int timestamp;
    String title = "";
    String url   = "";

    public Link() {
    }

    public Link(Link link) {
        copy(link);
    }

    public Link(int linkId, String category, int timestamp, String title, String url) {
        this.linkId = linkId;
        this.category = category;
        this.timestamp = timestamp;
        this.title = title;
        this.url = url;
    }

    public Link(String url) {
        this.url = url;
    }

    @Override public String toString() {
        return "Link{" +
                "linkId=" + linkId +
                ", category='" + category + '\'' +
                ", timestamp=" + timestamp +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public void copy(Link link) {
        linkId = link.linkId;
        category = link.category;
        timestamp = link.timestamp;
        title = link.title;
        url = link.url;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public int getLinkId() {
        return linkId;
    }

    public void setLinkId(int id) {
        this.linkId = id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return !TextUtils.isEmpty(title) ? title : getDomain();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
