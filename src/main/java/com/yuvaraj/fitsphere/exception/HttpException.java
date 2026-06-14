package com.yuvaraj.fitsphere.exception;

import org.springframework.http.HttpStatus;

/** Expected, client-facing error carrying an HTTP status (mirrors the Node HttpError). */
public class HttpException extends RuntimeException {

    private final HttpStatus status;

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpException(int status, String message) {
        super(message);
        this.status = HttpStatus.valueOf(status);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
