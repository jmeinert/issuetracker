package de.jmeinert.issuetracker.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Issue Tracker API",
        description = "REST API for managing projects and tracking issues.",
        version = "0.0.1"
    )
)
@SecurityScheme(
    name = OpenApiConfig.JWT_BEARER_SECURITY_SCHEME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT returned by `POST /api/auth/login`"
)
public class OpenApiConfig {

    public static final String JWT_BEARER_SECURITY_SCHEME = "JWT-Bearer";
}
