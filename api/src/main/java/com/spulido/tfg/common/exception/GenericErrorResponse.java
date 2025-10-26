package com.spulido.tfg.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class GenericErrorResponse {

    public GenericErrorResponse(ErrorDetails description, List<FieldValidationError> fieldErrors) {
        this.description = description;
        this.validationErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }

    protected ErrorDetails description;
    protected List<FieldValidationError> validationErrors;
    protected LocalDateTime timestamp;

    public Map<String, Object> mapOf() {
        Map<String, Object> map = new HashMap<>();
        if (this.description != null) {
            map.put("description", this.description);
        }
        if (this.validationErrors != null) {
            map.put("validationErrors", this.validationErrors);
        }
        map.put("timestamp", this.timestamp);
        return map;
    }
}

@Getter
@Setter
@AllArgsConstructor
class FieldValidationError {
    private String field;
    private String error;
}