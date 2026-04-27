package com.arena.app.models;

import com.google.gson.annotations.SerializedName;

public class CompilerSubmissionResultResponse {
    private String token;
    private String stdout;
    private String stderr;

    @SerializedName("compile_output")
    private String compileOutput;

    private String message;
    private String time;
    private Integer memory;
    private Status status;

    public String getToken() {
        return token;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public String getCompileOutput() {
        return compileOutput;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public Integer getMemory() {
        return memory;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isProcessing() {
        return status != null && status.id <= 2;
    }

    public static class Status {
        private int id;
        private String description;

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }
    }
}
