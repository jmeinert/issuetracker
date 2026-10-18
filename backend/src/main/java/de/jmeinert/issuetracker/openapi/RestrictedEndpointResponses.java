package de.jmeinert.issuetracker.openapi;

import de.jmeinert.issuetracker.error.ErrorResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@UnauthorizedResponse
@ApiResponse(
    responseCode = "403",
    description = "Access denied",
    content = @Content(
        schema = @Schema(implementation = ErrorResponse.class)
    )
)
public @interface RestrictedEndpointResponses {
}
