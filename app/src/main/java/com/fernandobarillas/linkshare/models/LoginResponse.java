package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 2/2/16.
 */
public class LoginResponse {
    boolean successful;
    String  authToken;
    String  username;

    @Override
    public String toString() {
        return "LoginResponse{" +
                "successful=" + successful +
                ", authToken='" + authToken + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
