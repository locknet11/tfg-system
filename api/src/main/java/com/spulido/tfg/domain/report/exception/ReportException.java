package com.spulido.tfg.domain.report.exception;

import com.spulido.tfg.common.exception.ErrorCode;

/**
 * Exception thrown for report generation and retrieval failures.
 */
public class ReportException extends RuntimeException {

    private final ErrorCode errorCode;

    public ReportException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
