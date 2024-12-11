package com.fps.svmes.dto.responses;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class ResponseResult<T> {
    /**
     * Response timestamp.
     */
    private long timestamp;

    /**
     * Response code, 200 -> OK.
     */
    private String status;

    /**
     * Response message.
     */
    private String message;

    /**
     * Response data.
     */
    private T data;

    public ResponseResult(long timestamp, String status, String message, T data) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * Success response result wrapper with no data.
     *
     * @param <T> type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success() {
        return success(null);
    }

    /**
     * Success response result wrapper with data.
     *
     * @param data response data
     * @param <T>  type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success(T data) {
        return ResponseResult.<T>builder().data(data)
                .message(ResponseStatus.SUCCESS.getDescription())
                .status(ResponseStatus.SUCCESS.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Error response result wrapper with message and exception.
     *
     * @param message error message
     * @param e       exception (nullable)
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> fail(String message, Exception e) {
        logException(message, e);
        return buildErrorResponse(null, message, ResponseStatus.FAIL.getResponseCode());
    }

    /**
     * Error response result wrapper with message and no exception.
     *
     * @param message error message
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> fail(String message) {
        return fail(message, null);
    }

    /**
     * Error response result wrapper with data, message, and exception.
     *
     * @param data    response data
     * @param message error message
     * @param e       exception (nullable)
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> fail(T data, String message, Exception e) {
        logException(message, e);
        return buildErrorResponse(data, message, ResponseStatus.FAIL.getResponseCode());
    }

    /**
     * Not found response result wrapper.
     *
     * @param message error message
     * @param e       exception (nullable)
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> failNotFound(String message, Exception e) {
        logException(message, e);
        return buildErrorResponse(null, message, ResponseStatus.HTTP_STATUS_404.getResponseCode());
    }

    /**
     * Bad request response result wrapper.
     *
     * @param message error message
     * @param e       exception (nullable)
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> failBadRequest(String message, Exception e) {
        log.error("{}: {}", message, e.getMessage());

        return ResponseResult.<T>builder()
                .message(message)
                .status(ResponseStatus.HTTP_STATUS_400.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Forbidden response result wrapper.
     *
     * @param message error message
     * @param e       exception (nullable)
     * @param <T>     type of data class
     * @return response result
     */
    public static <T> ResponseResult<T> failForbidden(String message, Exception e) {
        logException(message, e);
        return buildErrorResponse(null, message, ResponseStatus.HTTP_STATUS_403.getResponseCode());
    }

    /**
     * No content response result wrapper.
     *
     * @param <T>  type of data class
     * @param data response data (can be null)
     * @return response result
     */
    public static <T> ResponseResult<T> noContent(T data) {
        return ResponseResult.<T>builder()
                .data(data)
                .message(ResponseStatus.HTTP_STATUS_204.getDescription())
                .status(ResponseStatus.HTTP_STATUS_204.getResponseCode())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // Helper method to build error responses
    private static <T> ResponseResult<T> buildErrorResponse(T data, String message, String responseCode) {
        return ResponseResult.<T>builder()
                .data(data)
                .message(message)
                .status(responseCode)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // Helper method to log exceptions
    private static void logException(String message, Exception e) {
        if (e != null) {
            log.error("{}: {}", message, e.getMessage(), e);
        } else {
            log.error("Error: {}", message);
        }
    }
}
