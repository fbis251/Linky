package com.fernandobarillas.linkshare.api;

import android.text.TextUtils;

import com.fernandobarillas.linkshare.configuration.AppPreferences;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;
import com.fernandobarillas.linkshare.utils.OkHttpClientUtil;

import java.io.IOException;
import java.net.URL;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

/**
 * Created by fb on 3/5/17.
 */

public class LinkyService {
    private URL                  mApiUrl;
    private OkHttpClient.Builder mHttpClientBuilder;

    public LinkyService(final URL apiUrl) throws InvalidApiUrlException {
        Timber.v("LinkyService() called with: " + "apiUrl = [" + apiUrl + "]");
        validateApiUrl(apiUrl);
        mApiUrl = apiUrl;
        mHttpClientBuilder = new OkHttpClient.Builder();
    }

    public LinkyService(final URL apiUrl, final long userId, final String authToken)
            throws InvalidApiUrlException {
        Timber.v("LinkyService() called with: "
                + "apiUrl = ["
                + apiUrl
                + "], userId = ["
                + userId
                + "], authToken = [REDACTED]");
        validateApiUrl(apiUrl);
        mApiUrl = apiUrl;
        mHttpClientBuilder = new OkHttpClient.Builder();
        // Add the Basic Auth header to the request
        if (authToken != null) {
            mHttpClientBuilder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();

                    String userIdString = Long.valueOf(userId).toString();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", Credentials.basic(userIdString, authToken))
                            .method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            });
        }
    }

    public static void validateApiUrl(final URL apiUrl) throws InvalidApiUrlException {
        Timber.v("validateApiUrl() called with: " + "apiUrl = [" + apiUrl + "]");
        if (apiUrl == null || TextUtils.isEmpty(apiUrl.toString())) {
            throw new InvalidApiUrlException("Api URL cannot be blank");
        }

        String host = apiUrl.getHost();
        if (TextUtils.isEmpty(host.trim())) {
            throw new InvalidApiUrlException("Invalid API URL " + apiUrl.toString());
        }
    }

    public String getApiUrlWithScheme() {
        String result = "";
        if (mApiUrl != null) {
            result = String.format("%s://%s", mApiUrl.getProtocol(), mApiUrl.getHost());
        }
        return result;
    }

    public LinkyApi getLinkService(final AppPreferences preferences) {
        if (preferences != null) {
            mHttpClientBuilder =
                    OkHttpClientUtil.debugConfiguration(preferences, mHttpClientBuilder);
        }
        OkHttpClient okHttpClient = mHttpClientBuilder.build();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().client(okHttpClient)
                .baseUrl(getApiBaseUrl(mApiUrl))
                .addConverterFactory(MoshiConverterFactory.create());
        return retrofitBuilder.build().create(LinkyApi.class);
    }

    private String getApiBaseUrl(URL apiUrl) {
        Timber.v("getApiBaseUrl() called with: " + "apiUrl = [" + apiUrl + "]");
        String result = apiUrl.toString() + LinkyApi.API_BASE_URL;
        Timber.v("getApiBaseUrl: API base URL: " + result);
        return result;
    }
}
