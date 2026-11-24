package com.swyp.invite.global.error;

import com.swyp.invite.global.error.ApiErrorResponse.InvalidField;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        return response(e.getErrorCode(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(MethodArgumentNotValidException e) {
        List<InvalidField> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameter(HandlerMethodValidationException e) {
        List<InvalidField> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors body
                        ? body.getFieldErrors().stream()
                                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                        : result.getResolvableErrors().stream()
                                .map(error -> new InvalidField(
                                        result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return response(ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return response(ErrorCode.VALIDATION_FAILED, List.of(new InvalidField(e.getName(), "형식이 올바르지 않습니다")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return response(ErrorCode.VALIDATION_FAILED, List.of(new InvalidField(e.getParameterName(), "값이 필요합니다")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return response(ErrorCode.VALIDATION_FAILED, List.of());
    }

    private ResponseEntity<ApiErrorResponse> response(ErrorCode code, List<InvalidField> errors) {
        return ResponseEntity.status(code.getStatus())
                .body(new ApiErrorResponse(code.name(), code.getMessage(), errors));
    }
}
