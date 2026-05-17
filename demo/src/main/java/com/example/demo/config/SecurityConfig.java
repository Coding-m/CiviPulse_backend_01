package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity   // ✅ Enables @PreAuthorize across controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ---------------- PASSWORD ENCODER ----------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ---------------- AUTH MANAGER ----------------
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // ---------------- CORS CONFIGURATION ----------------
    // ✅ Proper CORS bean instead of empty cors() lambda
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ For production on Render, replace "*" with your actual frontend URL
        // e.g. "https://your-frontend.onrender.com"
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // ✅ Required for JWT in Authorization header
        config.setMaxAge(3600L);          // ✅ Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ---------------- SECURITY FILTER CHAIN ----------------
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ Disable CSRF — REST APIs use JWT, not sessions
            .csrf(AbstractHttpConfigurer::disable)

            // ✅ Use the CORS bean defined above
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ✅ Remove H2 frame options — not needed in production
            // .headers(headers -> headers.frameOptions().disable())

            // ✅ Stateless — no sessions, JWT only
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ---------------- AUTHORIZATION ----------------
            .authorizeHttpRequests(auth -> auth

                // ---------- PUBLIC ENDPOINTS ----------
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // ✅ CORS preflight first

                .requestMatchers(
                    "/api/admin/signup",
                    "/api/admin/login",
                    "/api/admin/forgot-password",
                    "/api/admin/reset-password"
                ).permitAll()

                .requestMatchers("/api/officer/login").permitAll()

                .requestMatchers(
                    "/api/citizen/signup",
                    "/api/citizen/login",
                    "/api/citizen/forgot-password",
                    "/api/citizen/reset-password"
                ).permitAll()

                // ✅ WebSocket
                .requestMatchers("/ws/**").permitAll()

                // ✅ Swagger — consider restricting in production
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // ✅ /error must be public so Spring can forward error responses
                .requestMatchers("/error").permitAll()

                // ✅ Removed /uploads/** — no longer needed, images served from Cloudinary
                // ✅ Removed /h2-console/** — not used in production

                // ---------- SPECIFIC RULES FIRST ----------
                .requestMatchers("/api/admin/officers/workload-summary").hasRole("ADMIN")
                .requestMatchers("/api/officer/workload").hasAnyRole("OFFICER", "ADMIN")

                // ---------- ROLE-BASED ----------
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/officer/**").hasRole("OFFICER")
                .requestMatchers("/api/citizen/**").hasRole("CITIZEN")

                // ---------- EVERYTHING ELSE ----------
                .anyRequest().authenticated()
            )

            // ✅ JWT filter runs before username/password filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}