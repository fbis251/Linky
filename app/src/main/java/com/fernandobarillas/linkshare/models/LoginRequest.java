package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/2/16.
 */
public class LoginRequest {
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
