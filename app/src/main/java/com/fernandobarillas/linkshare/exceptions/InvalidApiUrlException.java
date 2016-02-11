package com.fernandobarillas.linkshare.exceptions;

/**
 * Created by fb on 2/7/16.
 */
public class InvalidApiUrlException extends Exception {
    private static final String ERROR_MESSAGE = "Invalid API URL";

    public InvalidApiUrlException() {
        super(ERROR_MESSAGE);
    }
}
