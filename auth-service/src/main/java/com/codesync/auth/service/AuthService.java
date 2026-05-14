package com.codesync.auth.service;

import com.codesync.auth.dto.AuthResponse;
import com.codesync.auth.dto.LoginRequest;
import com.codesync.auth.dto.RegisterRequest;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.exception.UnauthorizedException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final String superAdminEmail;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       @Value("${app.admin.super-email:}") String superAdminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.superAdminEmail = superAdminEmail == null ? "" : superAdminEmail.trim();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByRazorpaySubscriptionId(String subscriptionId) {
        return userRepository.findByRazorpaySubscriptionId(subscriptionId).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }
        String username = request.username() != null ? request.username() : request.email().split("@")[0];
        User user = User.builder()
                .email(request.email().toLowerCase())
                .username(username)
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .avatar(request.avatar())
                .bio(request.bio())
                .provider(AuthProvider.LOCAL)
            .role(isSuperAdminEmail(request.email()) ? UserRole.ADMIN : UserRole.DEVELOPER)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        emailService.sendRegistrationEmail(user.getEmail(), user.getFullName());
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName());
        return buildResponse(user);
    }

    public AuthResponse buildOAuthResponse(User user) {
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtUtil.generateToken(
                user.getEmail(),
                Map.of("userId", user.getId(), "provider", user.getProvider().name(), "name", user.getFullName()));
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername(), user.getFullName(), user.getAvatar(), user.getBio(), user.getRole().name());
    }

    private boolean isSuperAdminEmail(String email) {
        return !superAdminEmail.isBlank() && superAdminEmail.equalsIgnoreCase(email);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null || user.getProvider() != AuthProvider.LOCAL) {
            return;
        }
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
    }

    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid request"));
        if (user.getPasswordResetToken() == null || !user.getPasswordResetToken().equals(token)) {
            throw new BadRequestException("Invalid reset token");
        }
        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
    }
}
