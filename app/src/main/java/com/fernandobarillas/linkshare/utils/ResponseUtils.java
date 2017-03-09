package com.fernandobarillas.linkshare.utils;

import retrofit2.Response;

/**
 * Created by fb on 2/1/16.
 */
public class ResponseUtils {
    // HTTP Status Codes
    private static final int STATUS_UNAUTHORIZED = 401;

    public static String httpCodeString(final Response response) {
        if (response == null) {
            return "";
        }

        return String.format("%d %s", response.code(), response.message());
    }

    public static boolean isAuthenticationError(final Response response) {
        if (response == null) {
            return false;
        }
        return response.code() == STATUS_UNAUTHORIZED;
    }
}
