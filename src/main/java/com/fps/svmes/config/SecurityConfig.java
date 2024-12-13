package com.fps.svmes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())        // Enable CORS
                .csrf(csrf -> csrf.disable())           // Disable CSRF using the new lambda style
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()           // Allow full access to all endpoints
                );
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Use allowedOriginPatterns instead of allowedOrigins
        config.setAllowedOriginPatterns(Arrays.asList("*"));   // Allow all origins with patterns
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allow common methods
        config.setAllowedHeaders(Arrays.asList("*"));          // Allow all headers
        config.setAllowCredentials(true);                      // Allow credentials (cookies, authorization headers)

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
