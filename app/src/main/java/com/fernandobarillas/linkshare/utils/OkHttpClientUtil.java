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
 * Created by fb on 3/17/17.
 */

public class OkHttpClientUtil {
    public static OkHttpClient.Builder debugConfiguration(
            AppPreferences appPreferences, OkHttpClient.Builder okHttpClientBuilder) {
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

        if (appPreferences.isLogHttpCalls()) {
            Timber.i("debugConfiguration: Logging all HTTP calls");
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(logging);
        }

        return okHttpClientBuilder;
    }
}
