package de.jmeinert.issuetracker.error;

import de.jmeinert.issuetracker.issue.ClosedIssueUpdateException;
import de.jmeinert.issuetracker.issue.InvalidIssueStatusTransitionException;
import de.jmeinert.issuetracker.issue.IssueNotFoundException;
import de.jmeinert.issuetracker.project.ProjectNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleProjectNotFound(ProjectNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(IssueNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIssueNotFound(IssueNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ClosedIssueUpdateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleClosedIssueUpdateException(ClosedIssueUpdateException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(InvalidIssueStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidIssueStatusTransitionException(InvalidIssueStatusTransitionException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value"),
                (message1, message2) -> message1
            ));

        return new ErrorResponse("Validation failed", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return new ErrorResponse("Invalid request body");
    }
}
