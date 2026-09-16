package de.jmeinert.issuetracker.issue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Operation(
    parameters = {
        @Parameter(
            name = "size",
            in = ParameterIn.QUERY,
            description = "Number of issues per page. Maximum value: 100."
        ),
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description = "Allowed sort properties are `createdAt`, `updatedAt` and `title`. "
                + "Sorting criteria format: property,(ASC|DESC). "
                + "Multiple sort criteria are supported."
        )
    }
)
public @interface IssuePaginationDocumentation {

    @AliasFor(annotation = Operation.class)
    String summary() default "";
}
