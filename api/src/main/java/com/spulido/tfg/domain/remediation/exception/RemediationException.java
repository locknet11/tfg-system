package com.spulido.tfg.domain.remediation.exception;

import com.spulido.tfg.common.exception.ErrorCode;

/**
 * Exception thrown when remediation operations fail.
 */
public class RemediationException extends RuntimeException {

    private final ErrorCode errorCode;

    public RemediationException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public RemediationException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RemediationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
