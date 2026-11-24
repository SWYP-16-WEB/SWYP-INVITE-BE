package com.swyp.invite.global.security;

import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    private static final String[] NO_COOKIE_APIS = {"/api/v1/invitations/**", "/api/v1/host/**", "/api/v1/images"};

    private static final String[] API_DOCS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors)
            throws Exception {
        AuthenticationEntryPoint unauthenticated = (request, response, e) ->
                errors.resolveException(request, response, null, new ApiException(ErrorCode.UNAUTHENTICATED));
        AccessDeniedHandler denied = (request, response, e) -> {
            ErrorCode code = e instanceof CsrfException ? ErrorCode.CSRF_TOKEN_INVALID : ErrorCode.FORBIDDEN;
            errors.resolveException(request, response, null, new ApiException(code));
        };

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(NO_COOKIE_APIS).permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/events").permitAll();
            auth.requestMatchers(API_DOCS).permitAll();
            auth.requestMatchers("/error").permitAll();
            auth.anyRequest().authenticated();
        });
        http.csrf(csrf -> csrf.spa().ignoringRequestMatchers(NO_COOKIE_APIS));
        http.requestCache(cache -> cache.disable());
        http.logout(logout -> {
            logout.logoutUrl("/api/v1/auth/logout");
            logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT));
        });
        http.exceptionHandling(handling -> {
            handling.authenticationEntryPoint(unauthenticated);
            handling.accessDeniedHandler(denied);
        });
        return http.build();
    }
}
