package io.github.avin15cs.authserver.config;

import io.github.avin15cs.authserver.security.PersistingOidcUserService;
import io.github.avin15cs.authserver.security.AuthAdminAuthorizationManager;
import io.github.avin15cs.authserver.client.ClientMetadataRepository;
import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServer =
                new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher(authorizationServer.getEndpointsMatcher())
                .with(authorizationServer, server -> server.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            PersistingOidcUserService oidcUserService,
            AuthAdminAuthorizationManager authAdminAuthorization) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .requestMatchers(
                                "/", "/index.html", "/dashboard", "/clients", "/clients/**", "/users",
                                "/assets/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                                .access(authAdminAuthorization)
                        .requestMatchers("/api/session").authenticated()
                        .requestMatchers("/api/admin/**").access(authAdminAuthorization)
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.userInfoEndpoint(userInfo ->
                        userInfo.oidcUserService(oidcUserService)))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny()))
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(AuthServerProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
            AuthUserRepository users,
            ClientMetadataRepository clientMetadata,
            AuthUserRoleRepository roles) {
        return context -> {
            boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
            boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
            if (!idToken && !accessToken) {
                return;
            }

            Map<String, Object> claims = new LinkedHashMap<>();
            if (accessToken) {
                claims.put("aud", audienceFor(clientMetadata, context.getRegisteredClient().getId()));
            }

            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                claims.put("subject_type", "client");
                claims.put("client_id", context.getRegisteredClient().getClientId());
                context.getClaims().claims(existing -> existing.putAll(claims));
                return;
            }

            if (!(context.getPrincipal().getPrincipal() instanceof OidcUser principal)) {
                return;
            }
            AuthUser user = users.findByProviderSubject("google", principal.getSubject()).orElse(null);
            if (user == null) {
                return;
            }

            claims.put("uid", user.id().toString());
            claims.put("subject_type", "user");
            if (context.getAuthorizedScopes().contains(OidcScopes.EMAIL)) {
                claims.put("email", user.email());
                claims.put("email_verified", user.emailVerified());
            }
            if (context.getAuthorizedScopes().contains(OidcScopes.PROFILE)) {
                claims.put("name", user.displayName());
                claims.put("picture", user.pictureUrl());
            }
            if (accessToken) {
                var assignedRoles = roles.findRoles(
                        user.id(), audienceFor(clientMetadata, context.getRegisteredClient().getId()));
                if (!assignedRoles.isEmpty()) {
                    claims.put("roles", assignedRoles);
                }
            }
            context.getClaims().claims(existing -> existing.putAll(claims));
        };
    }

    private static String audienceFor(ClientMetadataRepository metadata, String registeredClientId) {
        return metadata.findByRegisteredClientId(registeredClientId)
                .map(client -> client.audience())
                .orElseThrow(() -> new IllegalStateException("Client audience metadata is missing"));
    }
}
