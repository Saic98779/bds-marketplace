package com.sidbi.bds.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        List<ApiError> errors,
        Instant timestamp,
        String path
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, "Success", data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, 201, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> message(String message) {
        return new ApiResponse<>(true, 200, message, null, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(int status, String message, List<ApiError> errors, String path) {
        return new ApiResponse<>(false, status, message, null, errors, Instant.now(), path);
    }
}
