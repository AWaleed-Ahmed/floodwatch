/*
 * File:    RequestSizeFilter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Rejects request bodies above the configured size limit with HTTP 413.
 */


package com.floodwatch.config;

import com.floodwatch.controller.ApiError;
import com.floodwatch.service.PayloadTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;


/**
 * Rejects request bodies larger than the limit. Bodies that declare their length are refused
 * before reading; bodies without a length are counted while read and stopped at the limit.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class RequestSizeFilter extends OncePerRequestFilter {


    private final long maxBytes;
    private final JsonMapper json;
    private final Clock clock;

    /**
     * Creates the filter.
     *
     * @param props application settings holding the body size limit
     * @param json  JSON writer for the error response
     * @param clock clock for the error time stamp
     */


    public RequestSizeFilter
    (
        AppProperties props,
        JsonMapper json,
        Clock clock
    ) {
        this.maxBytes = props.http().maxBodyBytes();
        this.json = json;
        this.clock = clock;
    }

    /**
     * Refuses oversized bodies and wraps the rest with a counting stream.
     *
     * @param request  the incoming request
     * @param response the outgoing response
     * @param chain    the rest of the filter chain
     * @throws ServletException when a later filter fails
     * @throws IOException      when reading or writing fails
     */


    @Override
    protected void doFilterInternal
    (
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes)
        {
            reject(request, response);
            return;
        }
        chain.doFilter(new LimitedRequest(request, maxBytes), response);
    }

    /**
     * Writes the HTTP 413 error response.
     *
     * @param request  the rejected request
     * @param response the response to write
     * @throws IOException when writing fails
     */


    private void reject
    (
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        HttpStatus status = HttpStatus.CONTENT_TOO_LARGE;
        ApiError body = new ApiError(limitMessage(maxBytes), status.value(), request.getRequestURI(), clock.instant(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json.writeValueAsString(body));
    }

    /**
     * Builds the message shown when a body is too large.
     *
     * @param limit the limit in bytes
     * @return the message
     */


    static String limitMessage
    (
        long limit
    ) {
        return "Request body is larger than the " + limit / 1024 + " KB limit.";
    }


    /**
     * Request wrapper whose body stream stops at the size limit.
     *
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    private static final class LimitedRequest extends HttpServletRequestWrapper {


        private final long limit;

        /**
         * Wraps a request.
         *
         * @param request the original request
         * @param limit   maximum body size in bytes
         */


        LimitedRequest
        (
            HttpServletRequest request,
            long limit
        ) {
            super(request);
            this.limit = limit;
        }

        /**
         * Returns a body stream that throws once the limit is passed.
         *
         * @return the limited stream
         * @throws IOException when the original stream cannot be opened
         */


        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedStream(super.getInputStream(), limit);
        }
    }


    /**
     * Input stream that counts bytes and stops at the size limit.
     *
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    private static final class LimitedStream extends ServletInputStream {


        private final ServletInputStream delegate;
        private final long limit;
        private long count;

        /**
         * Wraps a stream.
         *
         * @param delegate the original stream
         * @param limit    maximum number of bytes to allow
         */


        LimitedStream
        (
            ServletInputStream delegate,
            long limit
        ) {
            this.delegate = delegate;
            this.limit = limit;
        }

        /**
         * Reads one byte and enforces the limit.
         *
         * @return the byte, or -1 at the end of the stream
         * @throws IOException when the original stream fails
         */


        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1)
            {
                count++;
                if (count > limit)
                {
                    throw new PayloadTooLargeException(limitMessage(limit));
                }
            }
            return value;
        }

        /**
         * Tells whether the whole body has been read.
         *
         * @return true at the end of the body
         */


        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        /**
         * Tells whether data can be read without blocking.
         *
         * @return true when data is ready
         */


        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        /**
         * Passes the read listener to the original stream.
         *
         * @param listener the listener for non-blocking reads
         */


        @Override
        public void setReadListener
        (
            ReadListener listener
        ) {
            delegate.setReadListener(listener);
        }
    }
}