package com.spulido.tfg.domain.alerts.exception;

public class AlertException extends Exception {

    private static final long serialVersionUID = 1L;

    public AlertException(String message) {
        super(message);
    }

    public AlertException(String message, Throwable cause) {
        super(message, cause);
    }
}
