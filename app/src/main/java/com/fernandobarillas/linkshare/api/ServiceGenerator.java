package com.fernandobarillas.linkshare.api;

import android.text.TextUtils;

import com.fernandobarillas.linkshare.configuration.Constants;
import com.fernandobarillas.linkshare.exceptions.InvalidApiUrlException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

/**
 * Created by fb on 2/1/16.
 */
public class ServiceGenerator {
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    public static <S> S createService(Class<S> serviceClass, final URL apiUrl) throws InvalidApiUrlException {
        return createService(serviceClass, apiUrl, null);
    }

    public static <S> S createService(Class<S> serviceClass, final URL apiUrl, final String refreshToken) throws InvalidApiUrlException {
        if (apiUrl == null || TextUtils.isEmpty(apiUrl.toString())) {
            throw new InvalidApiUrlException();
        }

        if (refreshToken != null) {
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();

                    Request.Builder requestBuilder =
                            original.newBuilder().header("Authorization", refreshToken).header("Accept", "applicaton/json").method(original.method(), original.body());

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
            client = httpClient.proxy(proxy).build();
        } else {
            client = httpClient.build();
        }

        String baseUrl = getApiUrlString(apiUrl);
        Retrofit.Builder builder =
                new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }

    private static String getApiUrlString(URL apiUrl) {
        return apiUrl.getProtocol() + "://" + apiUrl.getHost() + "/api/";
    }
}
