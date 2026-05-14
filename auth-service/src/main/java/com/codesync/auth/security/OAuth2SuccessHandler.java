package com.codesync.auth.security;

import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            String login = String.valueOf(principal.getAttributes().getOrDefault("login", "github-user"));
            email = login + "@github.local";
        }
        email = email.toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            String name = principal.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = String.valueOf(principal.getAttributes().getOrDefault("login", "OAuth User"));
            }
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .passwordHash(passwordEncoder.encode("oauth2-user"))
                    .provider(com.codesync.auth.entity.AuthProvider.LOCAL)
                    .build();
            user = userRepository.save(user);
        }
        String token = jwtUtil.generateToken(user.getEmail(), Map.of("userId", user.getId(), "name", user.getFullName()));
        response.sendRedirect(redirectUri + "?token=" + token);
    }
}
