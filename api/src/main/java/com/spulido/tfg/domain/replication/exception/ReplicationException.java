package com.spulido.tfg.domain.replication.exception;

import com.spulido.tfg.common.exception.ErrorCode;

import lombok.Getter;

@Getter
public class ReplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ReplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
