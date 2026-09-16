package de.jmeinert.issuetracker.openapi;

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
@ApiResponse(responseCode = "201", description = "Resource created", useReturnTypeSchema = true)
public @interface CreatedResponse {

    @AliasFor(annotation = ApiResponse.class)
    String description() default "Resource created";
}
