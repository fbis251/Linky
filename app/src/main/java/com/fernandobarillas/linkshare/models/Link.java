package com.fernandobarillas.linkshare.models;


import com.orm.SugarRecord;
import com.orm.dsl.Table;
import com.orm.dsl.Unique;

/**
 * Created by fb on 1/28/16.
 */
@Table
public class Link extends SugarRecord {
    @Unique
    int linkId;
    String category = "";
    int timestamp;
    String title = "";
    String url = "";

    public Link() {
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

    public int getLinkId() {
        return linkId;
    }

    public void setLinkId(int id) {
        this.linkId = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
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
