package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/10/16.
 */
public class AddLinkResponse {
    private boolean successful;
    private Link    link;

    public Link getLink() {
        return link;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
