package com.sidbi.bds.config;

import com.sidbi.bds.common.security.BdsSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, BdsSecurity bdsSecurity) throws Exception {
        return bdsSecurity.build(http,
                "/api/v1/auth/public-key",
                "/api/v1/auth/otp/send",
                "/api/v1/auth/register/**",
                "/api/v1/auth/login",
                "/api/v1/auth/login/verify",
                "/api/v1/auth/refresh",
                "/api/v1/auth/password/forgot",
                "/api/v1/auth/password/reset",
                "/api/v1/auth/meta");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${bds.web.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
