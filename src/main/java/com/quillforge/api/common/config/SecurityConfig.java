package com.quillforge.api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth and registration
                        .requestMatchers("/users/login", "/users/register", "/users/refresh-token",
                                "/users/forgot-password", "/users/reset-password", "/users/verify-invitation",
                                "/users/social/**").permitAll()
                        // Public storefront paths
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/pages", "/pages/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/blogs", "/blogs/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/comments").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/comments").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/admin/blogs/import/template").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/blogs/*/rate", "/blogs/*/views", "/blogs/*/likes", "/blogs/*/read-progress", "/blogs/slug/views/**", "/blogs/slug/read-progress/**", "/pages/*/views", "/pages/slug/views/**", "/pages/*/read-progress", "/pages/slug/read-progress/**").permitAll()
                        // Public documentation
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html", "/error").permitAll()
                        // Any other request must be authenticated
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
