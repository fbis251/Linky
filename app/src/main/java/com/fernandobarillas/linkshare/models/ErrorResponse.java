package com.fernandobarillas.linkshare.models;


/**
 * Created by fb on 6/29/16.
 */
public class ErrorResponse {
    private String errorMessage;

    @Override
    public String toString() {
        return "ErrorResponse{" + "errorMessage='" + errorMessage + '\'' + '}';
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
