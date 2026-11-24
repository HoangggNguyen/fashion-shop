package com.shop.common.api;

import java.util.List;

public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, List.of()));
    }

    public static <T> ApiResponse<T> error(String code, String message, List<String> details) {
        return new ApiResponse<>(false, null, new ApiError(code, message, details == null ? List.of() : details));
    }
}
