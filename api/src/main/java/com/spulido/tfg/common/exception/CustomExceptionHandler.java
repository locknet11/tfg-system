package com.spulido.tfg.common.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.mongodb.MongoWriteException;
import com.spulido.tfg.common.LangUtils;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.alerts.exception.AlertException;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.user.exception.UserException;
import com.spulido.tfg.domain.exploitation.exception.ExploitationKnowledgeException;
import com.spulido.tfg.domain.remediation.exception.RemediationException;
import com.spulido.tfg.domain.report.exception.ReportException;
import com.spulido.tfg.domain.replication.exception.ReplicationException;
import com.spulido.tfg.domain.vulnerability.exception.VulnerabilityException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class CustomExceptionHandler {

	private final LangUtils langUtils;

	@ExceptionHandler(MongoWriteException.class)
	protected ResponseEntity<?> handleMongoWriteException(MongoWriteException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.INTERNAL_ERROR)
				.detail(ErrorCode.INTERNAL_ERROR.getDefaultMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error(ex.toString());
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(DuplicateKeyException.class)
	protected ResponseEntity<?> duplicatedKey(DuplicateKeyException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.RESOURCE_ALREADY_EXISTS)
				.detail(ex.getMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error(ex.toString());
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.MESSAGE_NOT_READABLE)
				.detail(ErrorCode.MESSAGE_NOT_READABLE.getDefaultMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(BadCredentialsException.class)
	protected ResponseEntity<?> badCredentials(BadCredentialsException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.BAD_CREDENTIALS)
				.detail(ErrorCode.BAD_CREDENTIALS.getDefaultMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<?> accessDenied(AccessDeniedException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.UNAUTHORIZED)
				.detail(ErrorCode.UNAUTHORIZED.getDefaultMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	/*
	 * Throws error if request body is violating constraints on params anottated
	 * with @Valid
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, Locale locale) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.MISSING_REQUEST_PARAMETERS)
				.detail(ErrorCode.MISSING_REQUEST_PARAMETERS.getDefaultMessage()).build();
		if (ex.getFieldErrors() != null) {
			List<FieldValidationError> fieldValidationErrors = new ArrayList<>();
			ex.getFieldErrors().forEach(fieldError -> {
				fieldValidationErrors
						.add(new FieldValidationError(fieldError.getField(),
								langUtils.getLocalizedMessage(fieldError.getDefaultMessage(), locale)));
			});
			response = new GenericErrorResponse(errorDetails, fieldValidationErrors).mapOf();
		}
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(UserException.class)
	public ResponseEntity<?> handleUserExceptions(UserException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.USER_EXCEPTION)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(AgentException.class)
	public ResponseEntity<?> handleAgentExceptions(AgentException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.AGENT_EXCEPTION)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(TemplateException.class)
	public ResponseEntity<?> handleTemplateExceptions(TemplateException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.BAD_REQUEST)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(AlertException.class)
	public ResponseEntity<?> handleAlertExceptions(AlertException ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.BAD_REQUEST)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(ExploitationKnowledgeException.class)
	public ResponseEntity<?> handleExploitationKnowledgeExceptions(ExploitationKnowledgeException ex) {
		Map<String, Object> body = new java.util.HashMap<>();
		body.put("exceptionCode", ex.getExceptionCode());
		body.put("message", ex.getLocalizedMessage());
		body.put("timestamp", java.time.Instant.now().toString());

		HttpStatus status;
		switch (ex.getExceptionCode()) {
			case "NO_EXPLOIT_AVAILABLE":
			case "NO_SERVICE_DATA":
			case "AGENT_NOT_FOUND":
			case "TARGET_NOT_FOUND":
				status = HttpStatus.BAD_REQUEST;
				break;
			case "TARGET_NOT_AUTHORIZED":
				status = HttpStatus.FORBIDDEN;
				break;
			case "EXTERNAL_SERVICE_UNAVAILABLE":
				status = HttpStatus.BAD_GATEWAY;
				break;
			default:
				status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		log.error("Exploitation knowledge exception: {} - {}", ex.getExceptionCode(), ex.getMessage());
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(ReplicationException.class)
	public ResponseEntity<?> handleReplicationExceptions(ReplicationException ex) {
		HttpStatus status;
		switch (ex.getErrorCode()) {
			case REPLICATION_TOKEN_NOT_FOUND:
				status = HttpStatus.NOT_FOUND;
				break;
			case REPLICATION_TOKEN_EXPIRED:
				status = HttpStatus.GONE;
				break;
			case REPLICATION_TOKEN_CONSUMED:
				status = HttpStatus.FORBIDDEN;
				break;
			case REPLICATION_REQUEST_NOT_PENDING:
			case REPLICATION_DUPLICATE_REQUEST:
				status = HttpStatus.CONFLICT;
				break;
			case REPLICATION_REQUEST_NOT_FOUND:
			case REPLICATION_POLICY_NOT_FOUND:
				status = HttpStatus.NOT_FOUND;
				break;
			default:
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				break;
		}
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ex.getErrorCode())
				.detail(ex.getMessage()).build();
		log.error("Replication exception: {}", ex.getMessage());
		return ResponseEntity.status(status).body(new GenericErrorResponse(errorDetails, null).mapOf());
	}

	@ExceptionHandler(VulnerabilityException.class)
	public ResponseEntity<?> handleVulnerabilityExceptions(VulnerabilityException ex) {
		boolean isNotFound = ex.getMessage() != null && ex.getMessage().contains("not found");
		ErrorCode code = isNotFound ? ErrorCode.VULNERABILITY_NOT_FOUND : ErrorCode.EXTERNAL_API_ERROR;
		HttpStatus status = isNotFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;

		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(code)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error("Vulnerability exception: {}", ex.getMessage());
		return ResponseEntity.status(status).body(response);
	}

	@ExceptionHandler(RemediationException.class)
	public ResponseEntity<?> handleRemediationExceptions(RemediationException ex) {
		ErrorCode code = ex.getErrorCode();
		HttpStatus status;
		
		switch (code) {
			case REMEDIATION_NOT_FOUND:
				status = HttpStatus.NOT_FOUND;
				break;
			case REMEDIATION_STRATEGY_NOT_FOUND:
				status = HttpStatus.NOT_FOUND;
				break;
			case REMEDIATION_INVALID_STATUS:
				status = HttpStatus.BAD_REQUEST;
				break;
			case BAD_REQUEST:
			case INVALID_FIELD_VALUE:
				status = HttpStatus.BAD_REQUEST;
				break;
			default:
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				break;
		}

		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(code)
				.detail(ex.getLocalizedMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error("Remediation exception: {} - {}", code, ex.getMessage());
		return ResponseEntity.status(status).body(response);
	}

	@ExceptionHandler(ReportException.class)
	public ResponseEntity<?> handleReportExceptions(ReportException ex) {
		ErrorCode code = ex.getErrorCode();
		HttpStatus status;

		switch (code) {
			case REPORT_NOT_FOUND:
				status = HttpStatus.NOT_FOUND;
				break;
			case REPORT_EMPTY_RESULT:
			case REPORT_NO_PROJECT_CONTEXT:
				status = HttpStatus.UNPROCESSABLE_ENTITY;
				break;
			default:
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				break;
		}

		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(code)
				.detail(ex.getLocalizedMessage()).build();
		Map<String, Object> response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error("Report exception: {} - {}", code, ex.getMessage());
		return ResponseEntity.status(status).body(response);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<?> handleException(Exception ex) {
		Map<String, Object> response = null;
		ErrorDetails errorDetails = ErrorDetails.builder()
				.code(ErrorCode.INTERNAL_ERROR)
				.detail(ErrorCode.INTERNAL_ERROR.getDefaultMessage()).build();
		response = new GenericErrorResponse(errorDetails, null).mapOf();
		log.error(ex.toString());
		return ResponseEntity.badRequest().body(response);
	}

}
