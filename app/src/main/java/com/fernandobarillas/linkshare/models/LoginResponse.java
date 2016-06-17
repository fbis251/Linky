package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/2/16.
 */
public class LoginResponse {
    boolean success;
    String  refreshToken;
    String  username;

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSuccess() {
        return success;
    }
}
