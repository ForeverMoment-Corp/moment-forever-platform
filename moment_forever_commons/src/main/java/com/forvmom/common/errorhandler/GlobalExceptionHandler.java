package com.forvmom.common.errorhandler;

import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1️⃣ Handle DTO validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ResponseUtil.buildValidationErrorResponse("Validation failed", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());
        log.warn("Binding failed: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ResponseUtil.buildValidationErrorResponse("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        log.warn("Constraint violation: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ResponseUtil.buildValidationErrorResponse("Validation failed", errors));
    }

    // 2️⃣ Handle Business Validation logic exceptions
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ResponseUtil.buildBadRequestResponse(ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ResponseUtil.buildConflictResponse(ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyRequestInProgressException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyRequestInProgress(
            IdempotencyRequestInProgressException ex
    ) {
        log.info("Idempotent request still in progress: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("Retry-After", "1")
                .body(ResponseUtil.buildConflictResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ResponseUtil.buildErrorResponse(ex.getMessage(), ex.getStatus()));
    }

    // 3️⃣ Handle resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseUtil.buildNotFoundResponse(ex.getMessage()));
    }

    @ExceptionHandler(CustomAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomAuthException(CustomAuthException ex) {
        log.warn("Authentication/authorization failure: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ResponseUtil.buildErrorResponse(ex.getMessage(), ex.getStatus()));
    }

    @ExceptionHandler(NotAllowedCustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAllowed(NotAllowedCustomException ex) {
        log.warn("Operation not allowed: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ResponseUtil.buildErrorResponse(ex.getMessage(), ex.getStatus()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ResponseUtil.buildBadRequestResponse(resolveBadRequestMessage(ex)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String supportedMethods = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().stream().map(Enum::name).collect(Collectors.joining(", "));
        String message = supportedMethods.isBlank()
                ? "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint"
                : "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. Supported methods: "
                + supportedMethods;
        log.warn("Method not allowed: {}", message);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseUtil.buildErrorResponse(message, HttpStatus.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        String supportedTypes = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        String message = supportedTypes.isBlank()
                ? "Unsupported Content-Type"
                : "Unsupported Content-Type. Supported types: " + supportedTypes;
        log.warn("Unsupported media type: {}", message);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseUtil.buildErrorResponse(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ResponseUtil.buildErrorResponse("Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected server error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.buildErrorResponse("Unexpected server error. Please contact support if the problem persists.",
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private String resolveBadRequestMessage(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException missingParam) {
            return "Required request parameter '" + missingParam.getParameterName() + "' is missing";
        }
        if (ex instanceof MissingRequestHeaderException missingHeader) {
            return "Required request header '" + missingHeader.getHeaderName() + "' is missing";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            String requiredType = mismatch.getRequiredType() == null ? "required type"
                    : mismatch.getRequiredType().getSimpleName();
            return "Request parameter '" + mismatch.getName() + "' must be of type " + requiredType;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed or unreadable request body";
        }
        return ex.getMessage() == null ? "Bad request" : ex.getMessage();
    }
}