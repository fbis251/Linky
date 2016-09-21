package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/2/16.
 */
public class LoginResponse {
    public static final long INVALID_USER_ID = -1;

    private long userId = INVALID_USER_ID;
    private String authString;
    private String username;

    @Override
    public String toString() {
        return "LoginResponse{" +
                "userId=" + userId +
                ", authString='" + authString + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    public String getAuthString() {
        return authString;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
