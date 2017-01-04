package com.fernandobarillas.linkshare.api;

import com.fernandobarillas.linkshare.models.AddLinkRequest;
import com.fernandobarillas.linkshare.models.AddLinkResponse;
import com.fernandobarillas.linkshare.models.Link;
import com.fernandobarillas.linkshare.models.LoginRequest;
import com.fernandobarillas.linkshare.models.SuccessResponse;
import com.fernandobarillas.linkshare.models.UserInfoResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by fb on 1/28/16.
 */
public interface LinkService {
    String API_BASE_URL = "/api/1/"; // Must have trailing slash

    @POST("links")
    Call<AddLinkResponse> addLink(@Body AddLinkRequest linkRequest);

    @PUT("links/{linkId}/archive")
    Call<SuccessResponse> archiveLink(@Path("linkId") long linkId);

    @DELETE("links/{linkId}")
    Call<SuccessResponse> deleteLink(@Path("linkId") long linkId);

    @PUT("links/{linkId}/favorite")
    Call<SuccessResponse> favoriteLink(@Path("linkId") long linkId);

    @GET("links")
    Call<List<Link>> getLinks();

    @POST("login")
    Call<ResponseBody> login(@Body LoginRequest loginRequest);

    @DELETE("links/{linkId}/archive")
    Call<SuccessResponse> unarchiveLink(@Path("linkId") long linkId);

    @DELETE("links/{linkId}/favorite")
    Call<SuccessResponse> unfavoriteLink(@Path("linkId") long linkId);

    @PUT("links/{linkId}")
    Call<AddLinkResponse> updateLink(@Path("linkId") long linkId, @Body AddLinkRequest linkRequest);

    @GET("user_info")
    Call<UserInfoResponse> getUserInfo();
}
