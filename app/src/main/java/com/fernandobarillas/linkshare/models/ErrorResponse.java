package com.fernandobarillas.linkshare.models;

/**
 * Created by fb on 6/29/16.
 */
public class ErrorResponse {
    String statusMessage;

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "statusMessage='" + statusMessage + '\'' +
                '}';
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
