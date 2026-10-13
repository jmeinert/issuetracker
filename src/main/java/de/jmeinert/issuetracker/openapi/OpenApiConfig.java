package de.jmeinert.issuetracker.openapi;

import de.jmeinert.issuetracker.error.ErrorResponse;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocAnnotationsUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Issue Tracker API",
        description = "REST API for managing projects and tracking issues.",
        version = "1.0.0"
    )
)
@SecurityScheme(
    name = OpenApiConfig.COOKIE_AUTH_SECURITY_SCHEME,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "JSESSIONID",
    description = "Session cookie created by `POST /api/auth/login`"
)
public class OpenApiConfig {

    public static final String COOKIE_AUTH_SECURITY_SCHEME = "cookieAuth";

    @Bean
    OpenApiCustomizer authenticationEndpointsCustomizer() {
        var csrfMethods = Set.of(
            PathItem.HttpMethod.POST,
            PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH,
            PathItem.HttpMethod.DELETE
        );

        return openApi -> {
            var errorSchema = SpringDocAnnotationsUtils.extractSchema(
                openApi.getComponents(),
                ErrorResponse.class,
                null,
                null,
                openApi.getSpecVersion()
            );

            // Document additional authentication endpoints
            openApi.path("/api/auth/login", new PathItem().post(loginOperation(errorSchema)));
            openApi.path("/api/auth/logout", new PathItem().post(logoutOperation(errorSchema)));

            // Add X-XSRF-TOKEN header to all requests using CSRF-protected HTTP methods
            openApi.getPaths().values().forEach(path ->
                path.readOperationsMap().forEach((method, operation) -> {
                    if (csrfMethods.contains(method)) {
                        operation.addParametersItem(new HeaderParameter()
                            .name("X-XSRF-TOKEN")
                            .required(true)
                            .description("Value of the XSRF-TOKEN cookie")
                            .schema(new StringSchema()));
                    }
                })
            );
        };
    }

    private Operation loginOperation(Schema<?> errorSchema) {
        return new Operation()
            .operationId("login")
            .tags(List.of("Authentication"))
            .summary("Log in")
            .requestBody(loginRequestBody())
            .responses(new ApiResponses()
                .addApiResponse("204", noContentApiResponse())
                .addApiResponse("401", errorApiResponse(errorSchema, "Invalid username or password"))
                .addApiResponse("403", errorApiResponse(errorSchema, "Access denied"))
            );
    }

    private Operation logoutOperation(Schema<?> errorSchema) {
        return new Operation()
            .operationId("logout")
            .tags(List.of("Authentication"))
            .summary("Log out")
            .responses(new ApiResponses()
                .addApiResponse("204", noContentApiResponse())
                .addApiResponse("403", errorApiResponse(errorSchema, "Access denied"))
            );
    }

    private RequestBody loginRequestBody() {
        var schema = new ObjectSchema();
        schema.addProperty("username", new StringSchema());
        schema.addProperty("password", new StringSchema()
            .format("password")
            .description("Password of the user account")
        );
        schema.required(List.of("username", "password"));

        return new RequestBody()
            .required(true)
            .content(new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                new MediaType().schema(schema)
            ));
    }

    private ApiResponse noContentApiResponse() {
        return new ApiResponse().description("No content");
    }

    private ApiResponse errorApiResponse(Schema<?> errorSchema, String description) {
        return new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(errorSchema)
            ));
    }
}
