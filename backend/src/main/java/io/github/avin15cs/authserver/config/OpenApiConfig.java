package io.github.avin15cs.authserver.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PATCH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authServerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Server Management API")
                        .version("v1")
                        .description("Administrative API for sessions, OAuth clients, and audience-scoped user roles. "
                                + "Access requires a same-origin Google-authenticated AUTH_ADMIN session. "
                                + "Call GET /api/session to obtain the CSRF header name and token before mutations."))
                .externalDocs(new ExternalDocumentation()
                        .description("Authorization server architecture and security boundaries")
                        .url("https://github.com/avin15cs/auth-server/blob/dev/docs/ARCHITECTURE.md"));
    }

    @Bean
    OpenApiCustomizer csrfHeaderCustomizer() {
        var mutatingMethods = EnumSet.of(POST, PUT, PATCH, DELETE);
        return openApi -> openApi.getPaths().values().forEach(path ->
                path.readOperationsMap().forEach((method, operation) -> {
                    if (mutatingMethods.contains(method)) {
                        operation.addParametersItem(new Parameter()
                                .in("header")
                                .name("X-CSRF-TOKEN")
                                .required(true)
                                .description("Copy the token returned by GET /api/session.")
                                .schema(new StringSchema()));
                    }
                }));
    }
}
