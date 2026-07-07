package de.jmeinert.issuetracker.project;

import de.jmeinert.issuetracker.error.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleProjectNotFound(ProjectNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }
}
