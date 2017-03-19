package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/10/16.
 */
public class AddLinkRequest {
    private final Link link;

    public AddLinkRequest(Link link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "AddLinkRequest{" + "link=" + link + '}';
    }
}
