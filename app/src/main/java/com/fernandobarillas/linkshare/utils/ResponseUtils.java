package com.fernandobarillas.linkshare.utils;

import retrofit2.Response;

/**
 * Created by fb on 2/1/16.
 */
public class ResponsePrinter {
    public static String httpCodeString(Response response) {
        if (response == null) {
            return "";
        }

        return String.format("%d %s", response.code(), response.message());
    }
}
