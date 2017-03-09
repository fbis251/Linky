package com.fernandobarillas.linkshare.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by fb on 6/29/16.
 */
public class ErrorResponse {
    @SerializedName("errorMessage")
    private String errorMessage;

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
