package com.example.demo.security;

import com.example.demo.entity.Role;
import com.example.demo.repositories.AdminRepository;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.repositories.OfficerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CitizenRepository citizenRepository;
    private final OfficerRepository officerRepository;
    private final AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // ✅ Removed /uploads/ bypass — Cloudinary serves images now, no local paths

        String authHeader = request.getHeader("Authorization");

        // No token — continue filter chain (public endpoints handled by SecurityConfig)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // ✅ Validate token before extracting claims
        if (!jwtUtils.validateToken(token)) {
            log.warn("Invalid JWT token for request: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtils.extractEmail(token);
        String roleFromToken = jwtUtils.extractRole(token);

        if (email == null || roleFromToken == null) {
            log.warn("Missing claims in JWT for request: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Only set auth if not already set
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Normalize role — strip ROLE_ prefix if present
        String cleanRole = roleFromToken.startsWith("ROLE_")
                ? roleFromToken.substring(5)
                : roleFromToken;

        Role roleEnum;
        try {
            roleEnum = Role.valueOf(cleanRole.toUpperCase()); // ✅ toUpperCase for safety
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role '{}' in JWT for email: {}", cleanRole, email);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Load the correct user entity based on role
        Object user = switch (roleEnum) {
            case CITIZEN -> citizenRepository.findByEmail(email);
            case OFFICER -> officerRepository.findByEmail(email);
            case ADMIN   -> adminRepository.findByEmail(email);
        };

        if (user == null) {
            log.warn("JWT valid but user not found in DB — email: {}, role: {}", email, roleEnum);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Set authentication in security context
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + roleEnum.name());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        user, null, Collections.singletonList(authority));

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("Authenticated user: {}, role: {}, path: {}", email, roleEnum, requestPath);

        filterChain.doFilter(request, response);
    }
}