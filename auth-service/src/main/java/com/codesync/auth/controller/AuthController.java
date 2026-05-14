package com.codesync.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.codesync.auth.dto.AdminAccessRequestCreate;
import com.codesync.auth.dto.AdminAccessRequestDto;
import com.codesync.auth.dto.AdminUpdateRoleRequest;
import com.codesync.auth.dto.AdminUpdateStatusRequest;
import com.codesync.auth.dto.AdminUserView;
import com.codesync.auth.dto.AuthResponse;
import com.codesync.auth.dto.ForgotPasswordRequest;
import com.codesync.auth.dto.LoginRequest;
import com.codesync.auth.dto.RegisterRequest;
import com.codesync.auth.dto.ResetPasswordRequest;
import com.codesync.auth.dto.UserProfileUpdateRequest;
import com.codesync.auth.service.EmailService;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.entity.AdminAccessRequest;
import com.codesync.auth.entity.AdminRequestStatus;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.entity.UserStatus;
import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.exception.UnauthorizedException;
import com.codesync.auth.repository.AdminAccessRequestRepository;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;
import com.codesync.auth.service.AnalyticsClient;
import com.codesync.auth.service.NotificationClient;
import org.springframework.beans.factory.annotation.Value;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAccessRequestRepository adminAccessRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private AnalyticsClient analyticsClient;

    @Autowired
    private EmailService emailService;

    @Value("${app.admin.super-email:}")
    private String superAdminEmail;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        
        // Check if user already exists (normalize to lowercase)
        if (userRepository.findByEmail(request.email().toLowerCase().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        
        // Check username uniqueness if provided
        String username = request.username() != null ? request.username() : request.email().split("@")[0];
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is already taken"));
        }
        
        // Hash the password BEFORE saving it
        String hashedPassword = passwordEncoder.encode(request.password());

        // Build the user with the HASHED password and DEVELOPER role by default
        User newUser = User.builder()
                .email(request.email().toLowerCase().trim())
                .username(username)
                .fullName(request.fullName())
                .passwordHash(hashedPassword)
                .avatar(request.avatar())
                .bio(request.bio())
                .provider(AuthProvider.LOCAL)
            .role(isSuperAdminEmail(request.email()) ? UserRole.ADMIN : UserRole.DEVELOPER)
                .build();

        User savedUser = userRepository.save(newUser);
        emailService.sendRegistrationEmail(savedUser.getEmail(), savedUser.getFullName());
        
        // Generate JWT token with user info and role
        String token = jwtUtil.generateToken(
            savedUser.getEmail(),
            Map.of(
                "userId", savedUser.getId(),
                "email", savedUser.getEmail(),
                "fullName", savedUser.getFullName(),
                "role", savedUser.getRole().toString()
            )
        );

        AuthResponse response = new AuthResponse(
            token,
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getUsername(),
            savedUser.getFullName(),
            savedUser.getAvatar(),
            savedUser.getBio(),
            savedUser.getRole().toString()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/dev/create-admin")
    public ResponseEntity<?> createAdminDev(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email().toLowerCase().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        User newUser = User.builder()
                .email(request.email().toLowerCase().trim())
                .fullName(request.fullName())
                .passwordHash(hashedPassword)
                .provider(AuthProvider.LOCAL)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(newUser);
        String token = jwtUtil.generateToken(saved.getEmail(), 
            Map.of("userId", saved.getId(), "email", saved.getEmail(), "fullName", saved.getFullName()));
        return ResponseEntity.ok(new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getUsername(), saved.getFullName(), saved.getAvatar(), saved.getBio(), "ADMIN"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        
        // Find user by email (normalize to lowercase)
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Compare the raw typed password against the hashed database password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (isSuperAdminEmail(user.getEmail()) && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
        }

        emailService.sendLoginNotificationEmail(user.getEmail(), user.getFullName());

        // Generate JWT token with user info and role
        String token = jwtUtil.generateToken(
            user.getEmail(),
            Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().toString()
            )
        );

        AuthResponse response = new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getFullName(),
            user.getAvatar(),
            user.getBio(),
            user.getRole().toString()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim()).orElse(null);
        if (user != null && user.getProvider() == AuthProvider.LOCAL) {
            String token = java.util.UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiry(java.time.LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        }
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link will be sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid request"));
        
        if (user.getPasswordResetToken() == null || !user.getPasswordResetToken().equals(request.token())) {
            throw new BadRequestException("Invalid reset token");
        }
        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        
        String token = authHeader.substring(7);
        var claims = jwtUtil.extractAllClaims(token);
        Long userId = Long.parseLong(claims.get("userId").toString());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new BadRequestException("Username is already taken");
            }
            user.setUsername(request.username());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        
        User saved = userRepository.save(user);
        
        AuthResponse response = new AuthResponse(
            null,
            saved.getId(),
            saved.getEmail(),
            saved.getUsername(),
            saved.getFullName(),
            saved.getAvatar(),
            saved.getBio(),
            saved.getRole().toString()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        var claims = jwtUtil.extractAllClaims(token);
        Long userId = Long.parseLong(claims.get("userId").toString());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "email", user.getEmail(),
            "username", user.getUsername() != null ? user.getUsername() : "",
            "fullName", user.getFullName(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
            "bio", user.getBio() != null ? user.getBio() : "",
            "role", user.getRole().toString()
        ));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("valid", false));
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtUtil.validateToken(token);
        
        if (isValid) {
            var claims = jwtUtil.extractAllClaims(token);
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "email", claims.get("email"),
                "role", claims.get("role"),
                "userId", claims.get("userId")
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("valid", false));
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search query must be at least 2 characters"));
        }
        List<User> users = userRepository.searchUsers(q.trim());
        List<Map<String, Object>> results = users.stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "username", u.getUsername() != null ? u.getUsername() : "",
                "fullName", u.getFullName(),
                "avatar", u.getAvatar() != null ? u.getAvatar() : "",
                "role", u.getRole().toString()
            ))
            .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "email", user.getEmail(),
            "username", user.getUsername(),
            "fullName", user.getFullName(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
            "bio", user.getBio() != null ? user.getBio() : "",
            "role", user.getRole().toString()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) String excludeCurrentUser) {
        List<User> users;
        if ("true".equalsIgnoreCase(excludeCurrentUser)) {
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            users = userRepository.findByEmailNot(currentEmail);
        } else {
            users = userRepository.findAll();
        }
        List<Map<String, Object>> results = users.stream()
            .filter(u -> u.getRole() != UserRole.ADMIN)
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "fullName", u.getFullName(),
                "role", u.getRole().toString(),
                "status", u.getStatus().toString()
            ))
            .toList();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/admin-requests")
    public ResponseEntity<?> requestAdminAccess(@Valid @RequestBody AdminAccessRequestCreate request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Account not found"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getRole() != UserRole.DEVELOPER) {
            throw new BadRequestException("Only developer accounts can request admin access");
        }

        adminAccessRequestRepository.findByUserIdAndStatus(user.getId(), AdminRequestStatus.PENDING)
                .ifPresent((existing) -> {
                    throw new BadRequestException("An admin request is already pending");
                });

        AdminAccessRequest created = adminAccessRequestRepository.save(AdminAccessRequest.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(AdminRequestStatus.PENDING)
                .build());

        notifySuperAdmin(
            "ADMIN_REQUEST",
            user.getFullName() + " requested admin access.");

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping("/admin-requests")
    public ResponseEntity<?> listAdminRequests(@RequestHeader("X-Admin-Email") String adminEmail,
                                               @RequestParam(defaultValue = "PENDING") AdminRequestStatus status) {
        assertSuperAdmin(adminEmail);
        List<AdminAccessRequestDto> results = adminAccessRequestRepository
                .findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(results);
    }

@PostMapping("/admin-requests/{id}/approve")
    public ResponseEntity<?> approveAdminRequest(@RequestHeader("X-Admin-Email") String adminEmail,
                                                  @PathVariable Long id) {
        assertSuperAdmin(adminEmail);
        AdminAccessRequest request = adminAccessRequestRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Request not found"));
        if (request.getStatus() != AdminRequestStatus.PENDING) {
            throw new BadRequestException("Request is already processed");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        request.setStatus(AdminRequestStatus.APPROVED);
        request.setReviewedAt(java.time.LocalDateTime.now());
        request.setReviewedByEmail(adminEmail);
        AdminAccessRequest saved = adminAccessRequestRepository.save(request);
        notifyUser(request.getUserId(), "ADMIN_REQUEST_APPROVED", "Your admin access request was approved.");
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/admin-requests/{id}/deny")
    public ResponseEntity<?> denyAdminRequest(@RequestHeader("X-Admin-Email") String adminEmail,
                                              @PathVariable Long id) {
        assertSuperAdmin(adminEmail);
        AdminAccessRequest request = adminAccessRequestRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Request not found"));
        if (request.getStatus() != AdminRequestStatus.PENDING) {
            throw new BadRequestException("Request is already processed");
        }
        request.setStatus(AdminRequestStatus.DENIED);
        request.setReviewedAt(java.time.LocalDateTime.now());
        request.setReviewedByEmail(adminEmail);
        AdminAccessRequest saved = adminAccessRequestRepository.save(request);
        notifyUser(request.getUserId(), "ADMIN_REQUEST_DENIED", "Your admin access request was denied.");
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<AdminUserView>> listUsers(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        List<AdminUserView> users = userRepository.findAll().stream()
                .map(this::toUserView)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<AdminUserView> updateUserRole(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRoleRequest request) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail) && request.role() != UserRole.ADMIN) {
            throw new BadRequestException("You cannot remove your own admin role");
        }
        user.setRole(request.role());
        userRepository.save(user);
        return ResponseEntity.ok(toUserView(user));
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<AdminUserView> updateUserStatus(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateStatusRequest request) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail) && request.status() == UserStatus.SUSPENDED) {
            throw new BadRequestException("You cannot suspend your own account");
        }
        user.setStatus(request.status());
        userRepository.save(user);
        return ResponseEntity.ok(toUserView(user));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/users/count")
    public ResponseEntity<Long> getActiveUserCount(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        long count = userRepository.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/system/latency")
    public ResponseEntity<Long> getSystemLatency(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        return ResponseEntity.ok(10L);
    }

    private boolean isSuperAdminEmail(String email) {
        return superAdminEmail != null
                && !superAdminEmail.isBlank()
                && superAdminEmail.equalsIgnoreCase(email);
    }

    private void assertSuperAdmin(String adminEmail) {
        userRepository.findByEmail(adminEmail.toLowerCase().trim())
                .filter(user -> user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Admin access required"));
    }

    private void assertAdmin(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new UnauthorizedException("Admin access required");
        }
        userRepository.findByEmail(adminEmail.toLowerCase().trim())
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new UnauthorizedException("Admin access required"));
    }

    private AdminUserView toUserView(User user) {
        return new AdminUserView(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private AdminAccessRequestDto toDto(AdminAccessRequest request) {
        return new AdminAccessRequestDto(
                request.getId(),
                request.getUserId(),
                request.getEmail(),
                request.getFullName(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getReviewedByEmail()
        );
    }

    private void notifyUser(Long userId, String type, String message) {
        try {
            notificationClient.sendSystemNotification(userId, type, message);
        } catch (Exception ex) {
            // Intentionally ignore notification failures to avoid blocking admin actions.
        }
    }

    private void notifySuperAdmin(String type, String message) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            return;
        if (user.getRole() != UserRole.DEVELOPER) {
            throw new BadRequestException("Only developer accounts can request admin access");
        }

        adminAccessRequestRepository.findByUserIdAndStatus(user.getId(), AdminRequestStatus.PENDING)
                .ifPresent((existing) -> {
                    throw new BadRequestException("An admin request is already pending");
                });

        AdminAccessRequest created = adminAccessRequestRepository.save(AdminAccessRequest.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(AdminRequestStatus.PENDING)
                .build());

        notifySuperAdmin(
            "ADMIN_REQUEST",
            user.getFullName() + " requested admin access.");

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping("/admin-requests")
    public ResponseEntity<?> listAdminRequests(@RequestHeader("X-Admin-Email") String adminEmail,
                                               @RequestParam(defaultValue = "PENDING") AdminRequestStatus status) {
        assertSuperAdmin(adminEmail);
        List<AdminAccessRequestDto> results = adminAccessRequestRepository
                .findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(results);
    }

@PostMapping("/admin-requests/{id}/approve")
    public ResponseEntity<?> approveAdminRequest(@RequestHeader("X-Admin-Email") String adminEmail,
                                                  @PathVariable Long id) {
        assertSuperAdmin(adminEmail);
        AdminAccessRequest request = adminAccessRequestRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Request not found"));
        if (request.getStatus() != AdminRequestStatus.PENDING) {
            throw new BadRequestException("Request is already processed");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        request.setStatus(AdminRequestStatus.APPROVED);
        request.setReviewedAt(java.time.LocalDateTime.now());
        request.setReviewedByEmail(adminEmail);
        AdminAccessRequest saved = adminAccessRequestRepository.save(request);
        notifyUser(request.getUserId(), "ADMIN_REQUEST_APPROVED", "Your admin access request was approved.");
        return ResponseEntity.ok(toDto(saved));
    }

    @PostMapping("/admin-requests/{id}/deny")
    public ResponseEntity<?> denyAdminRequest(@RequestHeader("X-Admin-Email") String adminEmail,
                                              @PathVariable Long id) {
        assertSuperAdmin(adminEmail);
        AdminAccessRequest request = adminAccessRequestRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Request not found"));
        if (request.getStatus() != AdminRequestStatus.PENDING) {
            throw new BadRequestException("Request is already processed");
        }
        request.setStatus(AdminRequestStatus.DENIED);
        request.setReviewedAt(java.time.LocalDateTime.now());
        request.setReviewedByEmail(adminEmail);
        AdminAccessRequest saved = adminAccessRequestRepository.save(request);
        notifyUser(request.getUserId(), "ADMIN_REQUEST_DENIED", "Your admin access request was denied.");
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<AdminUserView>> listUsers(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        List<AdminUserView> users = userRepository.findAll().stream()
                .map(this::toUserView)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<AdminUserView> updateUserRole(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRoleRequest request) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail) && request.role() != UserRole.ADMIN) {
            throw new BadRequestException("You cannot remove your own admin role");
        }
        user.setRole(request.role());
        userRepository.save(user);
        return ResponseEntity.ok(toUserView(user));
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<AdminUserView> updateUserStatus(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateStatusRequest request) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail) && request.status() == UserStatus.SUSPENDED) {
            throw new BadRequestException("You cannot suspend your own account");
        }
        user.setStatus(request.status());
        userRepository.save(user);
        return ResponseEntity.ok(toUserView(user));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @PathVariable Long id) {
        assertAdmin(adminEmail);
        User user = userRepository.findById(id).orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/users/count")
    public ResponseEntity<Long> getActiveUserCount(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        long count = userRepository.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/system/latency")
    public ResponseEntity<Long> getSystemLatency(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        return ResponseEntity.ok(10L);
    }

    private boolean isSuperAdminEmail(String email) {
        return superAdminEmail != null
                && !superAdminEmail.isBlank()
                && superAdminEmail.equalsIgnoreCase(email);
    }

    private void assertSuperAdmin(String adminEmail) {
        userRepository.findByEmail(adminEmail.toLowerCase().trim())
                .filter(user -> user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Admin access required"));
    }

    private void assertAdmin(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new UnauthorizedException("Admin access required");
        }
        userRepository.findByEmail(adminEmail.toLowerCase().trim())
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new UnauthorizedException("Admin access required"));
    }

    private AdminUserView toUserView(User user) {
        return new AdminUserView(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private AdminAccessRequestDto toDto(AdminAccessRequest request) {
        return new AdminAccessRequestDto(
                request.getId(),
                request.getUserId(),
                request.getEmail(),
                request.getFullName(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getReviewedByEmail()
        );
    }

    private void notifyUser(Long userId, String type, String message) {
        try {
            notificationClient.sendSystemNotification(userId, type, message);
        } catch (Exception ex) {
            // Intentionally ignore notification failures to avoid blocking admin actions.
        }
    }

    private void notifySuperAdmin(String type, String message) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            return;
        }
        userRepository.findByEmail(superAdminEmail.toLowerCase().trim())
                .ifPresent(adminUser -> notifyUser(adminUser.getId(), type, message));
    }

    @GetMapping("/admin/analytics")
    public ResponseEntity<?> getPlatformAnalytics(@RequestHeader("X-Admin-Email") String adminEmail) {
        assertAdmin(adminEmail);
        
        long totalUsers = userRepository.count();
        
        Map<String, Object> projectStats = analyticsClient.getProjectStats();
        Map<String, Object> executionStats = analyticsClient.getExecutionStats();
        Map<String, Object> collabStats = analyticsClient.getCollabStats();
        
        Map<String, Long> languageBreakdown = new java.util.HashMap<>();
        if (projectStats.containsKey("languageBreakdown")) {
            Object langBreakdown = projectStats.get("languageBreakdown");
            if (langBreakdown instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Long> map = (java.util.Map<String, Long>) langBreakdown;
                languageBreakdown.putAll(map);
            }
        }

        Map<String, Long> execLanguageBreakdown = new java.util.HashMap<>();
        if (executionStats.containsKey("languageBreakdown")) {
            Object execLangBreakdown = executionStats.get("languageBreakdown");
            if (execLangBreakdown instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Number> map = (java.util.Map<String, Number>) execLangBreakdown;
                map.forEach((k, v) -> execLanguageBreakdown.put(k, v.longValue()));
            }
        }
        
        Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("totalUsers", totalUsers);
        analytics.put("totalProjects", projectStats.getOrDefault("totalProjects", 0L));
        analytics.put("totalExecutions", executionStats.getOrDefault("totalExecutions", 0L));
        analytics.put("avgExecutionTime", executionStats.getOrDefault("avgExecutionTime", 0L));
        analytics.put("totalSessions", collabStats.getOrDefault("totalSessions", 0L));
        analytics.put("languageBreakdown", languageBreakdown);
        analytics.put("execLanguageBreakdown", execLanguageBreakdown);
        
        return ResponseEntity.ok(analytics);
    }
}