package com.fernandobarillas.linkshare.models;

import java.util.Date;

/**
 * Created by fb on 2/10/16.
 */
public class UserInfoResponse {
    private long lastUpdateTimestamp;

    @Override
    public String toString() {
        return "UserInfoResponse{" +
                "lastUpdateTimestamp=" +
                lastUpdateTimestamp +
                "(" +
                new Date(lastUpdateTimestamp * 1000) + ")" +
                '}';
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }
}
