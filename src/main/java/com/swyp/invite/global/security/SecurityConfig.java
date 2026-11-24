package com.swyp.invite.global.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;

@Configuration
public class SecurityConfig {

	private static final String[] NO_COOKIE_APIS = { "/api/v1/invitations/**", "/api/v1/host/**", "/api/v1/images" };

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(NO_COOKIE_APIS).permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/events").permitAll()
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
				.anyRequest().authenticated())
			.csrf(csrf -> csrf.spa().ignoringRequestMatchers(NO_COOKIE_APIS))
			.requestCache(cache -> cache.disable())
			.logout(logout -> logout
				.logoutUrl("/api/v1/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint((request, response, e) -> errors.resolveException(request, response, null,
						new ApiException(ErrorCode.UNAUTHENTICATED)))
				.accessDeniedHandler((request, response, e) -> errors.resolveException(request, response, null,
						new ApiException(e instanceof CsrfException ? ErrorCode.CSRF_TOKEN_INVALID
								: ErrorCode.FORBIDDEN))));
		return http.build();
	}

}
