package com.fernandobarillas.linkshare.api;

import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.LinksList;
import com.fernandobarillas.linkshare.models.LoginRequest;
import com.fernandobarillas.linkshare.models.LoginResponse;
import com.fernandobarillas.linkshare.models.SuccessResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by fb on 1/28/16.
 */
public interface LinkShare {
    @POST("add")
    Call<Link> addLink(@Body AddLinkRequest linkRequest);

    @GET("archive/{id}")
    Call<SuccessResponse> archiveLink(@Path("id") int id);

    @GET("list")
    Call<LinksList> getList();

    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
}
