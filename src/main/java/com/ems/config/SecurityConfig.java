package com.ems.config;

import com.ems.security.JwtAccessDeniedHandler;
import com.ems.security.JwtAuthenticationEntryPoint;
import com.ems.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Web UI & static resources
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico",
                                "/h2-console/**"
                        ).permitAll()

                        // Public Auth APIs (Login & Initial setup)
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/me").authenticated()

                        // HR Dashboard -> Admin and HR only
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "HR")

                        // Department Management -> Admin and HR create/update, only Admin deletes
                        .requestMatchers(HttpMethod.POST, "/api/v1/departments/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/departments/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasRole("ADMIN")

                        // Employee Mutations -> Admin & HR can add; Admin, HR & Manager can edit; only Admin deletes
                        .requestMatchers(HttpMethod.POST, "/api/v1/employees/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/employees/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**").hasRole("ADMIN")

                        // Export CSV -> Admin and HR only
                        .requestMatchers("/api/v1/employees/export/csv").hasAnyRole("ADMIN", "HR")

                        // Leave Management permissions
                        .requestMatchers(HttpMethod.POST, "/api/v1/leaves").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/leaves/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/leaves/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/v1/leaves/stats").hasAnyRole("ADMIN", "HR", "MANAGER")

                        // Read APIs require any authenticated role (ADMIN, HR, MANAGER, EMPLOYEE)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        // Required if H2 console is used
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        // Add JWT token filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
