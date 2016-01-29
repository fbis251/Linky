package com.fernandobarillas.linkshare.api;

import com.fernandobarillas.linkshare.models.Link;

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by fb on 1/28/16.
 */
public interface LinkShare {
    String BASE_URL = "http://192.168.1.2:8000/";

    @GET("/list")
    Call<List<String>> getList();

    @GET("/delete/{id}")
    Call<String> getUser(@Path("id") int id);

    @POST("/add")
    Call<Link> addLink(@Body Link link);
}
