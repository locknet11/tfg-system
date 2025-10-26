package com.spulido.tfg.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
	INVALID_FIELD_VALUE("exception.invalidFieldValue"),
	RESOURCE_NOT_FOUND("exception.resourceNotFound"),
	BAD_REQUEST("exception.badRequest"),
	ILLEGAL_ARGUMENT("exception.illegalArgument"),
	HTTP_CLIENT_ERROR("exception.httpClientError"),
	BAD_CREDENTIALS("exception.badCredentials"),
	TOKEN_INVALID_OR_EXPIRED("exception.tokenInvalidOrExpired"),
	INTERNAL_ERROR("exception.internalError"),
	SERVICE_NOT_IMPLEMENTED("exception.serviceNotImplemented"),
	SERVICE_UNAVAILABLE("exception.serviceUnavailable"),
	METHOD_NOT_CURRENTLY_ALLOWED("exception.methodNotCurrentlyAllowed"),
	MEDIA_TYPE_NOT_SUPPORTED("exception.mediaTypeNotSupported"),
	MEDIA_TYPE_NOT_CURRENTLY_ALLOWED("exception.mediaTypeNotCurrentlyAllowed"),
	CONVERSION_NOT_SUPPORTED("exception.conversionNotSupported"),
	MISSING_PATH_VARIABLE("exception.missingPathVariable"),
	MISSING_REQUEST_PARAMETERS("exception.missingRequestParameters"),
	REQUEST_BINDING("exception.requestBinding"),
	MESSAGE_NOT_READABLE("exception.messageNotReadable"),
	MESSAGE_NOT_WRITABLE("exception.messageNotWritable"),
	MISSING_REQUEST_PART("exception.missingRequestPart"),
	NOT_HANDLER_FOUND("exception.notHandlerFound"),
	TYPE_MISMATCH("exception.typeMismatch"),
	PARAMS_REQUIRED("exception.paramsRequired"),
	ROLE_INVALID("exception.roleInvalid"),
	UNAUTHORIZED("exception.unauthorized"),
	USER_EXCEPTION("exception.userException"),
	RESOURCE_ALREADY_EXISTS("exception.resourceAlreadyExists");

	private final String defaultMessage;
}
