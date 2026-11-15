package com.sidbi.bds.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class BdsSecurity {

    private static final String[] DOC_PATHS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**", "/actuator/health", "/actuator/info", "/error"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final RestSecurityHandlers handlers;

    public BdsSecurity(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtFilter = new JwtAuthenticationFilter(jwtService);
        this.handlers = new RestSecurityHandlers(objectMapper);
    }

    public SecurityFilterChain build(HttpSecurity http, String... publicPaths) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.deny()))
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(DOC_PATHS).permitAll();
                    auth.requestMatchers("/api/v1/public/**").permitAll();
                    if (publicPaths.length > 0) {
                        auth.requestMatchers(publicPaths).permitAll();
                    }
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/seller/**").hasAnyRole("SELLER", "ADMIN");
                    auth.requestMatchers("/api/v1/buyer/**").hasAnyRole("BUYER", "ADMIN");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
