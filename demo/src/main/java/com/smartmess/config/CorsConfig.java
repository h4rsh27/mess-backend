package com.smartmess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Allowed front-end origins. In production set ALLOWED_ORIGINS env var to your
     * Vercel URL, e.g.: https://mess-frontend-mocha.vercel.app
     * Multiple origins can be comma-separated.
     * Defaults to "*" (open) so local development works without extra config.
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split comma-separated origins; support wildcard pattern
        List<String> origins = List.of(allowedOrigins.split(","));
        if (origins.size() == 1 && origins.get(0).equals("*")) {
            // allowedOriginPatterns("*") works with credentials; allowedOrigins("*") does NOT
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }

        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept",
            "X-Requested-With", "Origin"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setMaxAge(3600L); // 1 hour preflight cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
