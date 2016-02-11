package com.fernandobarillas.linkshare.databases;

import android.util.Log;

import com.fernandobarillas.linkshare.models.Link;
import com.orm.SugarRecord;

import java.util.List;

/**
 * Created by fb on 2/7/16.
 */
public class LinkStorage {
    private static final String LOG_TAG = "LinkStorage";

    public static List<Link> findByUrl(String url) {
        Log.v(LOG_TAG, "findByUrl() called with: " + "url = [" + url + "]");
        return Link.find(Link.class, "url = ?", url);
    }

    public static long getLinksCount() {
        return Link.count(Link.class);
    }

    public static List<Link> getAllLinks() {
        Log.v(LOG_TAG, "getAllLinks()");
        return Link.listAll(Link.class);
    }

    public static void replaceLinks(List<Link> newLinksList) {
        Log.v(LOG_TAG, "replaceLinks() called with: " + "newLinksList = [" + newLinksList + "]");
        Link.deleteAll(Link.class);
        SugarRecord.saveInTx(newLinksList);
    }
}
