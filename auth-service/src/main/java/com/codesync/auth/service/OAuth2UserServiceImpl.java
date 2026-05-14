package com.codesync.auth.service;

import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.repository.UserRepository;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String superAdminEmail;

    public OAuth2UserServiceImpl(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.admin.super-email:}") String superAdminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.superAdminEmail = superAdminEmail == null ? "" : superAdminEmail.trim();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmail(registrationId, attributes).toLowerCase();
        String name = extractName(registrationId, attributes);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .passwordHash(passwordEncoder.encode("oauth2-user"))
                    .provider("github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE)
                    .role(isSuperAdminEmail(email) ? UserRole.ADMIN : UserRole.DEVELOPER)
                    .build();
            return userRepository.save(newUser);
        });
        return oAuth2User;
    }

    private boolean isSuperAdminEmail(String email) {
        return !superAdminEmail.isBlank() && superAdminEmail.equalsIgnoreCase(email);
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        if ("github".equalsIgnoreCase(provider)) {
            Object email = attributes.get("email");
            return email == null ? attributes.getOrDefault("login", "github-user") + "@github.local" : email.toString();
        }
        return String.valueOf(attributes.get("email"));
    }

    private String extractName(String provider, Map<String, Object> attributes) {
        if ("github".equalsIgnoreCase(provider)) {
            return String.valueOf(attributes.getOrDefault("name", attributes.getOrDefault("login", "GitHub User")));
        }
        return String.valueOf(attributes.getOrDefault("name", "Google User"));
    }
}
