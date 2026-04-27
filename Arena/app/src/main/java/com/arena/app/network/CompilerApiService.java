package com.arena.app.network;

import com.arena.app.models.CompilerSubmissionRequest;
import com.arena.app.models.CompilerSubmissionResultResponse;
import com.arena.app.models.CompilerSubmissionTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CompilerApiService {

    @POST("submissions")
    Call<CompilerSubmissionTokenResponse> createSubmission(
            @Query("base64_encoded") boolean base64Encoded,
            @Query("wait") boolean wait,
            @Body CompilerSubmissionRequest request
    );

    @GET("submissions/{token}")
    Call<CompilerSubmissionResultResponse> getSubmission(
            @Path("token") String token,
            @Query("base64_encoded") boolean base64Encoded,
            @Query("fields") String fields
    );
}
