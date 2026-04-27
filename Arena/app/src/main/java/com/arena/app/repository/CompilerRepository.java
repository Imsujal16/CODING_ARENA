package com.arena.app.repository;

import android.os.Handler;
import android.os.Looper;

import com.arena.app.models.CodeLanguage;
import com.arena.app.models.CompilerSubmissionRequest;
import com.arena.app.models.CompilerSubmissionResultResponse;
import com.arena.app.models.CompilerSubmissionTokenResponse;
import com.arena.app.network.CompilerApiClient;
import com.arena.app.network.CompilerApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompilerRepository {
    private static final long POLL_DELAY_MS = 1500L;
    private static final int MAX_POLL_ATTEMPTS = 20;
    private static final String RESULT_FIELDS =
            "token,stdout,stderr,compile_output,message,time,memory,status";

    private final CompilerApiService apiService;
    private final Handler handler;

    public CompilerRepository() {
        apiService = CompilerApiClient.getInstance().getApiService();
        handler = new Handler(Looper.getMainLooper());
    }

    public void runCode(CodeLanguage language,
                        String sourceCode,
                        String stdin,
                        ExecutionCallback callback) {
        CompilerSubmissionRequest request = new CompilerSubmissionRequest(
                sourceCode,
                language.getLanguageId(),
                normalizeText(stdin)
        );

        apiService.createSubmission(false, false, request).enqueue(new Callback<CompilerSubmissionTokenResponse>() {
            @Override
            public void onResponse(Call<CompilerSubmissionTokenResponse> call,
                                   Response<CompilerSubmissionTokenResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !hasText(response.body().getToken())) {
                    callback.onError("The compiler service did not accept this run.");
                    return;
                }

                callback.onStatusChanged("Queued on compiler...");
                pollSubmission(response.body().getToken(), 0, callback);
            }

            @Override
            public void onFailure(Call<CompilerSubmissionTokenResponse> call, Throwable t) {
                callback.onError("Unable to reach the compiler service right now.");
            }
        });
    }

    public String buildConsoleOutput(CompilerSubmissionResultResponse result) {
        if (result == null) {
            return "No compiler output was returned.";
        }
        if (hasText(result.getCompileOutput())) {
            return result.getCompileOutput().trim();
        }
        if (hasText(result.getStderr())) {
            return result.getStderr().trim();
        }
        if (hasText(result.getMessage())) {
            return result.getMessage().trim();
        }
        if (hasText(result.getStdout())) {
            return result.getStdout().trim();
        }
        return "Program finished with no output.";
    }

    public boolean wasSuccessful(CompilerSubmissionResultResponse result) {
        return result != null
                && result.getStatus() != null
                && result.getStatus().getId() == 3
                && !hasText(result.getCompileOutput())
                && !hasText(result.getStderr())
                && !hasText(result.getMessage());
    }

    private void pollSubmission(String token, int attempt, ExecutionCallback callback) {
        if (attempt >= MAX_POLL_ATTEMPTS) {
            callback.onError("The compiler is taking too long. Please try again.");
            return;
        }

        apiService.getSubmission(token, false, RESULT_FIELDS)
                .enqueue(new Callback<CompilerSubmissionResultResponse>() {
                    @Override
                    public void onResponse(Call<CompilerSubmissionResultResponse> call,
                                           Response<CompilerSubmissionResultResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("The compiler returned an invalid result.");
                            return;
                        }

                        CompilerSubmissionResultResponse result = response.body();
                        if (result.isProcessing()) {
                            String description = result.getStatus() != null
                                    ? result.getStatus().getDescription()
                                    : "Processing";
                            callback.onStatusChanged(description + "...");
                            handler.postDelayed(
                                    () -> pollSubmission(token, attempt + 1, callback),
                                    POLL_DELAY_MS
                            );
                            return;
                        }

                        callback.onCompleted(result);
                    }

                    @Override
                    public void onFailure(Call<CompilerSubmissionResultResponse> call, Throwable t) {
                        callback.onError("The compiler result could not be fetched.");
                    }
                });
    }

    private String normalizeText(String value) {
        return hasText(value) ? value : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public interface ExecutionCallback {
        void onStatusChanged(String status);
        void onCompleted(CompilerSubmissionResultResponse result);
        void onError(String message);
    }
}
