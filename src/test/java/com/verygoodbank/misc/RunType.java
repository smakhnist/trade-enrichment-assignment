package com.verygoodbank.misc;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RunType {
    NAIVE("http://localhost:{port}/api/v1/enrich-naive"),
    READ_WRITE_SPLIT("http://localhost:{port}/api/v1/enrich-read-write-split"),
    EFFICIENT_STRUCTURES("http://localhost:{port}/api/v1/enrich-efficient-structures"),
    QUICK_ALL_IN_ONE("http://localhost:{port}/api/v1/enrich-quick");

    private final String path;

    public String getPath(int port) {
        return path.replace("{port}", port + "");
    }
}