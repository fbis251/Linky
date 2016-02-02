package com.fernandobarillas.linkshare.api;

import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.SuccessResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by fb on 1/28/16.
 */
public interface LinkShare {
    @POST("/add")
    Call<Link> addLink(@Body Link link);

    @GET("/delete/{id}")
    Call<SuccessResponse> deleteLink(@Path("id") int id);

    @GET("/list")
    Call<List<String>> getList();
}
