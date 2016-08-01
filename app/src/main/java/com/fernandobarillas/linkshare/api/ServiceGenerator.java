package com.fernandobarillas.linkshare.api;

import android.text.TextUtils;
import android.util.Log;

import com.fernandobarillas.linkshare.BuildConfig;
import com.fernandobarillas.linkshare.configuration.Constants;
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

/**
 * Created by fb on 2/1/16.
 */
public class ServiceGenerator {
    private static final String LOG_TAG = "ServiceGenerator";

    public static <S> S createService(Class<S> serviceClass, final URL apiUrl)
            throws InvalidApiUrlException {
        if (!isApiUrlValid(apiUrl)) {
            if (apiUrl != null) {
                throw new InvalidApiUrlException("Invalid API URL " + apiUrl.toString());
            } else {
                throw new InvalidApiUrlException();
            }
        }

        return createService(serviceClass, apiUrl, null);
    }

    public static <S> S createService(Class<S> serviceClass, final URL apiUrl,
            final String authToken) throws InvalidApiUrlException {
        if (!isApiUrlValid(apiUrl)) {
            throw new InvalidApiUrlException();
        }

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        // Add the Basic Auth header to the request
        if (authToken != null) {
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", Credentials.basic(authToken, ""))
                            .method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            });
        }

        OkHttpClient client;
        if (Constants.USE_HTTP_PROXY) {
            SocketAddress proxyAddress =
                    new InetSocketAddress(Constants.HTTP_PROXY_ADDRESS, Constants.HTTP_PROXY_PORT);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
            httpClient = httpClient.proxy(proxy);
        }

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);
        }

        client = httpClient.build();
        Gson gson = new GsonBuilder().serializeNulls().create();
        String baseUrl = getApiUrlString(apiUrl);
        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson));
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }

    public static boolean isApiUrlValid(final URL apiUrl) {
        Log.v(LOG_TAG, "isApiUrlValid() called with: " + "apiUrl = [" + apiUrl + "]");
        if (apiUrl != null && !TextUtils.isEmpty(apiUrl.toString())) {
            String host = apiUrl.getHost();
            if (!TextUtils.isEmpty(host.trim())) {
                return true;
            }
        }

        return false;
    }

    private static String getApiUrlString(URL apiUrl) {
        Log.v(LOG_TAG, "getApiUrlString() called with: " + "apiUrl = [" + apiUrl + "]");
        String result = apiUrl.toString() + LinkService.API_BASE_URL;
        Log.v(LOG_TAG, "getApiUrlString: API result URL: " + result);
        return result;
    }
}
