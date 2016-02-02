package com.fernandobarillas.linkshare.api;

import android.util.Base64;

import com.fernandobarillas.linkshare.configuration.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

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

    private static Retrofit.Builder builder =
            new Retrofit.Builder().baseUrl(Constants.API_BASE_URL).addConverterFactory(GsonConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null, null);
    }

    public static <S> S createService(Class<S> serviceClass, String username, String password) {
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            final String basic =
                    "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();

                    Request.Builder requestBuilder =
                            original.newBuilder().header("Authorization", basic).header("Accept", "applicaton/json").method(original.method(), original.body());

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

        Retrofit retrofit = builder.client(client).build();
        return retrofit.create(serviceClass);
    }
}
