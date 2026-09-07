package de.jmeinert.issuetracker.error;

import de.jmeinert.issuetracker.auth.UserAlreadyExistsException;
import de.jmeinert.issuetracker.issue.ClosedIssueUpdateException;
import de.jmeinert.issuetracker.issue.InvalidIssueStatusTransitionException;
import de.jmeinert.issuetracker.issue.InvalidSortFieldException;
import de.jmeinert.issuetracker.issue.IssueNotFoundException;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;
import de.jmeinert.issuetracker.project.ProjectHasIssuesException;
import de.jmeinert.issuetracker.user.UserDisabledException;
import de.jmeinert.issuetracker.user.UserNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        ProjectNotFoundException.class,
        IssueNotFoundException.class,
        UserNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler({
        ProjectHasIssuesException.class,
        ClosedIssueUpdateException.class,
        InvalidIssueStatusTransitionException.class,
        UserAlreadyExistsException.class,
        UserDisabledException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(InvalidSortFieldException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidSortFieldException(InvalidSortFieldException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value"),
                (message1, message2) -> message1
            ));

        return new ErrorResponse("Validation failed", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        Map<String, String> errors = e.getParameterValidationResults().stream()
            .collect(Collectors.toMap(
                result -> Objects.requireNonNullElse(
                    result.getMethodParameter().getParameterName(),
                    "Argument"
                ),
                result -> result.getResolvableErrors().stream()
                    .map(error -> Objects.requireNonNullElse(
                        error.getDefaultMessage(),
                        "Invalid value"
                    ))
                    .findFirst()
                    .orElse("Invalid value"),
                (message1, message2) -> message1
            ));

        return new ErrorResponse("Validation failed", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return new ErrorResponse("Invalid value '" + e.getValue() + "' for argument '" + e.getName() + "'.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return new ErrorResponse("Invalid request body");
    }

    @ExceptionHandler({
        BadCredentialsException.class,
        AccountStatusException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleLoginExceptions(RuntimeException e) {
        return new ErrorResponse("Invalid username or password");
    }
}
