package com.spulido.tfg.domain.user.exception;

public class UserException extends Exception {

    public UserException(String messageKey) {
        super(messageKey);
    }

}
