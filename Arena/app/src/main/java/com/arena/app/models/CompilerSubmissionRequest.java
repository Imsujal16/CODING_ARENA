package com.arena.app.models;

import com.google.gson.annotations.SerializedName;

public class CompilerSubmissionRequest {
    @SerializedName("source_code")
    private final String sourceCode;

    @SerializedName("language_id")
    private final int languageId;

    private final String stdin;

    public CompilerSubmissionRequest(String sourceCode, int languageId, String stdin) {
        this.sourceCode = sourceCode;
        this.languageId = languageId;
        this.stdin = stdin;
    }
}
