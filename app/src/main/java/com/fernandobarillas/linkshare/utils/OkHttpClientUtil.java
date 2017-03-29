package com.fernandobarillas.linkshare.utils;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.configuration.AppPreferences;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

/**
 * Provides utilities for manipulating OkHttp Clients and Builders
 */
public class OkHttpClientUtil {

    /**
     * Adds debugging options to an OkHttpClient.Builder instance if a user has requested them
     * through their Preferences and the Application is debuggable
     *
     * @param appPreferences      The user's preferences to get debug options from
     * @param okHttpClientBuilder The builder to add debugging options to
     * @return If the application is debuggable, a modified OkHttpClient.Builder instance with the
     * debug options the user chose in their Preferences, otherwise it returns the instance
     * unmodified
     */
    public static OkHttpClient.Builder debugConfiguration(AppPreferences appPreferences,
            OkHttpClient.Builder okHttpClientBuilder) {
        Timber.v("debugConfiguration() called with: "
                + "appPreferences = ["
                + appPreferences
                + "], okHttpClientBuilder = ["
                + okHttpClientBuilder
                + "]");
        if (!BuildConfig.DEBUG) return okHttpClientBuilder;

        if (appPreferences.isUseHttpProxy()) {
            Timber.i("debugConfiguration: Trying to use HTTP Proxy");
            try {
                String address = appPreferences.getHttpProxyAddress();
                int port = appPreferences.getHttpProxyPort();
                Timber.v("debugConfiguration: Proxy address: [%s:%d]", address, port);
                SocketAddress proxyAddress = new InetSocketAddress(address, port);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
                okHttpClientBuilder = okHttpClientBuilder.proxy(proxy);
            } catch (NumberFormatException e) {
                Timber.e(e, "debugConfiguration: ");
                // Invalid port number, can't set proxy
            }
        }

        if (!appPreferences.isLogErrorsOnly() && appPreferences.isLogHttpCalls()) {
            Timber.i("debugConfiguration: Logging all HTTP calls");
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(logging);
        }

        return okHttpClientBuilder;
    }
}
