package com.verygoodbank.misc;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RunType {
    NAIVE("http://localhost:{port}/api/v1/enrich-naive"),
    DF_THREAD_LOCAL("http://localhost:{port}/api/v1/enrich-df-thread-local"),
    EFFICIENT_STRUCTURES("http://localhost:{port}/api/v1/enrich-efficient-structures"),
    READ_WRITE_SPLIT("http://localhost:{port}/api/v1/enrich-threads-split");

    private final String path;

    public String getPath(int port) {
        return path.replace("{port}", port + "");
    }
}