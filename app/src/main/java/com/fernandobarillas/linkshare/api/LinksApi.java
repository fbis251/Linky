package com.fernandobarillas.linkshare.api;

import android.text.TextUtils;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * Created by fb on 3/5/17.
 */

public class LinksApi {
    private LinkService mLinkService;

    public LinksApi(final URL apiUrl) throws InvalidApiUrlException {
        Timber.v("LinksApi() called with: " + "apiUrl = [" + apiUrl + "]");
        validateApiUrl(apiUrl);
        buildService(apiUrl, new OkHttpClient.Builder());
    }

    public LinksApi(final URL apiUrl, final long userId, final String authToken)
            throws InvalidApiUrlException {
        Timber.v("LinksApi() called with: "
                + "apiUrl = ["
                + apiUrl
                + "], userId = ["
                + userId
                + "], authToken = [REDACTED]");
        validateApiUrl(apiUrl);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        // Add the Basic Auth header to the request
        if (authToken != null) {
            httpClient.addInterceptor(new Interceptor() {
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
        buildService(apiUrl, httpClient);
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

    public LinkService getLinkService() {
        return mLinkService;
    }

    private void buildService(final URL apiUrl, OkHttpClient.Builder okHttpClientBuilder) {
        if (BuildConfig.DEBUG && BuildConfig.USE_HTTP_PROXY) {
            SocketAddress proxyAddress = new InetSocketAddress(BuildConfig.HTTP_PROXY_ADDRESS,
                    BuildConfig.HTTP_PROXY_PORT);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
            okHttpClientBuilder = okHttpClientBuilder.proxy(proxy);
        }

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(logging);
        }

        OkHttpClient okHttpClient = okHttpClientBuilder.build();
        Gson gson = new GsonBuilder().serializeNulls().create();
        String baseUrl = getApiUrlString(apiUrl);
        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson));
        Retrofit retrofit = builder.client(okHttpClient).build();
        mLinkService = retrofit.create(LinkService.class);
    }

    private String getApiUrlString(URL apiUrl) {
        Timber.v("getApiUrlString() called with: " + "apiUrl = [" + apiUrl + "]");
        if (apiUrl == null) return null;
        String result = apiUrl.toString() + LinkService.API_BASE_URL;
        Timber.v("getApiUrlString: API result URL: " + result);
        return result;
    }
}
