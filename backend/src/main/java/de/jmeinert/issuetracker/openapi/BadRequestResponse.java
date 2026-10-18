package de.jmeinert.issuetracker.openapi;

import de.jmeinert.issuetracker.error.ErrorResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
    responseCode = "400",
    description = "Invalid request",
    content = @Content(
        schema = @Schema(implementation = ErrorResponse.class)
    )
)
public @interface BadRequestResponse {

    @AliasFor(annotation = ApiResponse.class)
    String description() default "Invalid request";
}
