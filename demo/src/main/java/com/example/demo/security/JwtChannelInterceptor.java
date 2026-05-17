package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message; // ✅ Early return — only process CONNECT frames
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT attempted without Authorization header");
            return message; // ✅ Let SecurityConfig handle unauthenticated access
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            log.warn("WebSocket CONNECT attempted with invalid JWT");
            return message;
        }

        String email = jwtUtils.extractEmail(token);
        if (email == null) {
            log.warn("WebSocket JWT missing email claim");
            return message;
        }

        // ✅ Set authenticated user on the WebSocket session
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email.toLowerCase(),
                        null,
                        Collections.emptyList()
                );

        accessor.setUser(authentication);
        log.info("WebSocket authenticated: {}", email);

        return message;
    }
}