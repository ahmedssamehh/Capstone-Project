package com.workhub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * 
 * Configures JWT-based authentication with stateless sessions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String TENANT_USER = "TENANT_USER";

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * Configure security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/metrics/**").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole(TENANT_ADMIN, TENANT_USER)
                        .requestMatchers(HttpMethod.GET, "/api/projects/**").hasAnyRole(TENANT_ADMIN, TENANT_USER)
                        .requestMatchers(HttpMethod.GET, "/api/tasks/**").hasAnyRole(TENANT_ADMIN, TENANT_USER)
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole(TENANT_ADMIN, TENANT_USER)
                        .requestMatchers(HttpMethod.GET, "/api/jobs/**").hasAnyRole(TENANT_ADMIN, TENANT_USER)
                        .requestMatchers(HttpMethod.GET, "/api/test/transaction/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/test/transaction/**").hasRole(TENANT_ADMIN)
                        .requestMatchers("/api/tenant/**").hasRole(TENANT_ADMIN)
                        .requestMatchers("/api/v1/tenants/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/projects/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/jobs/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/tasks/**").hasRole(TENANT_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasRole(TENANT_ADMIN)
                        .anyRequest().denyAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Authentication provider with custom user details service
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
