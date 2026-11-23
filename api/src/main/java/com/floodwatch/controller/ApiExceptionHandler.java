/*
 * File:    ApiExceptionHandler.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Turns every exception into the single JSON error format with the right status.
 */


package com.floodwatch.controller;

import com.floodwatch.integration.ExternalDataException;
import com.floodwatch.repository.CorruptDataException;
import com.floodwatch.repository.StorageLimitException;
import com.floodwatch.service.ApiException;
import com.floodwatch.service.PayloadTooLargeException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * Turns every exception into an ApiError with the right HTTP status. Unexpected errors are
 * logged in full but the caller only sees a generic message, so no internals leak out.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestControllerAdvice
public class ApiExceptionHandler {


    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String GENERIC_ERROR = "Something went wrong on the server. Please try again.";
    private static final String DATA_DAMAGED = "Some stored data is damaged and needs an administrator. Nothing was changed.";
    private static final String STORAGE_DOWN = "Data storage or an external service is unavailable. Please try again shortly.";

    private final Clock clock;

    /**
     * Creates the handler.
     *
     * @param clock clock used for error time stamps
     */


    public ApiExceptionHandler
    (
        Clock clock
    ) {
        this.clock = clock;
    }

    /**
     * Handles FloodWatch business errors, which carry their own status.
     *
     * @param e       the business error
     * @param request the failed request
     * @return the error response
     */


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException
    (
        ApiException e,
        HttpServletRequest request
    ) {
        return respond(e.getStatus(), e.getMessage(), request, List.of());
    }

    /**
     * Handles request bodies that fail bean validation, listing every bad field.
     *
     * @param e       the validation error
     * @param request the failed request
     * @return HTTP 400 with one detail per invalid field
     */


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody
    (
        MethodArgumentNotValidException e,
        HttpServletRequest request
    ) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .sorted()
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Some fields are invalid.", request, details);
    }

    /**
     * Handles invalid query or path parameters checked by method validation.
     *
     * @param e       the validation error
     * @param request the failed request
     * @return HTTP 400 with one detail per invalid parameter
     */


    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidParameter
    (
        HandlerMethodValidationException e,
        HttpServletRequest request
    ) {
        List<String> details = e.getAllErrors().stream()
                .map(error -> String.valueOf(error.getDefaultMessage()))
                .sorted()
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Some parameters are invalid.", request, details);
    }

    /**
     * Handles a required query parameter that is missing.
     *
     * @param e       the missing-parameter error
     * @param request the failed request
     * @return HTTP 400 naming the parameter
     */


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter
    (
        MissingServletRequestParameterException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.BAD_REQUEST, "Missing required parameter '" + e.getParameterName() + "'.", request, List.of());
    }

    /**
     * Handles a parameter of the wrong type, such as a bad date or an unknown enum value.
     *
     * @param e       the type error
     * @param request the failed request
     * @return HTTP 400 naming the parameter
     */


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch
    (
        MethodArgumentTypeMismatchException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.BAD_REQUEST, "Parameter '" + e.getName() + "' has an invalid value.", request, List.of());
    }

    /**
     * Handles a body that is not valid JSON, has wrong field types, or is over the size limit.
     *
     * @param e       the read error
     * @param request the failed request
     * @return HTTP 413 when the body was too large, otherwise HTTP 400
     */


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody
    (
        HttpMessageNotReadableException e,
        HttpServletRequest request
    ) {
        Throwable cause = e;
        while (cause != null)
        {
            if (cause instanceof PayloadTooLargeException tooLarge)
            {
                return respond(tooLarge.getStatus(), tooLarge.getMessage(), request, List.of());
            }
            cause = cause.getCause();
        }
        return respond(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON for this endpoint.", request, List.of());
    }

    /**
     * Handles a known path called with the wrong HTTP method.
     *
     * @param e       the method error
     * @param request the failed request
     * @return HTTP 405
     */


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleWrongMethod
    (
        HttpRequestMethodNotSupportedException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not allowed here.", request, List.of());
    }

    /**
     * Handles a body sent with an unsupported content type.
     *
     * @param e       the media type error
     * @param request the failed request
     * @return HTTP 415
     */


    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleWrongMediaType
    (
        HttpMediaTypeNotSupportedException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send the body as application/json.", request, List.of());
    }

    /**
     * Handles a path that does not exist.
     *
     * @param e       the missing-resource error
     * @param request the failed request
     * @return HTTP 404
     */


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleUnknownPath
    (
        NoResourceFoundException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.NOT_FOUND, "No endpoint at this path.", request, List.of());
    }

    /**
     * Handles a damaged data file: the request fails safely and the file is left untouched.
     *
     * @param e       the damaged-file error
     * @param request the failed request
     * @return HTTP 503 telling the caller the data needs an administrator
     */


    @ExceptionHandler(CorruptDataException.class)
    public ResponseEntity<ApiError> handleCorruptData
    (
        CorruptDataException e,
        HttpServletRequest request
    ) {
        LOG.error("Damaged data file {} on {}", e.getFileName(), request.getRequestURI());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, DATA_DAMAGED, request, List.of());
    }

    /**
     * Handles a save refused because a data file would pass the size limit.
     *
     * @param e       the size-limit error
     * @param request the failed request
     * @return HTTP 507 with the reason
     */


    @ExceptionHandler(StorageLimitException.class)
    public ResponseEntity<ApiError> handleStorageLimit
    (
        StorageLimitException e,
        HttpServletRequest request
    ) {
        return respond(HttpStatus.INSUFFICIENT_STORAGE, e.getMessage(), request, List.of());
    }

    /**
     * Handles storage and external service failures that reach a controller.
     *
     * @param e       the failure
     * @param request the failed request
     * @return HTTP 503
     */


    @ExceptionHandler({ExternalDataException.class, RestClientException.class, UncheckedIOException.class})
    public ResponseEntity<ApiError> handleUnavailable
    (
        RuntimeException e,
        HttpServletRequest request
    ) {
        LOG.warn("Dependency failure on {}: {}", request.getRequestURI(), e.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, STORAGE_DOWN, request, List.of());
    }

    /**
     * Handles anything unexpected without leaking internal details.
     *
     * @param e       the unexpected error
     * @param request the failed request
     * @return HTTP 500 with a generic message
     */


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected
    (
        Exception e,
        HttpServletRequest request
    ) {
        LOG.error("Unexpected error on {}", request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_ERROR, request, List.of());
    }

    /**
     * Builds the error response.
     *
     * @param status  HTTP status to send
     * @param message message for the caller
     * @param request the failed request
     * @param details field-level details
     * @return the response entity
     */


    private ResponseEntity<ApiError> respond
    (
        HttpStatus status,
        String message,
        HttpServletRequest request,
        List<String> details
    ) {
        Instant now = clock.instant();
        ApiError body = new ApiError(message, status.value(), request.getRequestURI(), now, details);
        return ResponseEntity.status(status).body(body);
    }
}