package com.project.app.common;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

@Component
public class LangUtils {

    @Autowired
    private MessageSource messageSource;
    private final String ERROR_MESSAGE_NOT_FOUND = "Error message not found";

    public String getLocalizedMessage(String keyMessage, Locale locale) throws NoSuchMessageException {

        if (keyMessage.isBlank()) {
            return ERROR_MESSAGE_NOT_FOUND;
        }

        try {
            return messageSource.getMessage(keyMessage, null, locale);
        } catch (NoSuchMessageException e) {
            return keyMessage;
        }
    }

}
