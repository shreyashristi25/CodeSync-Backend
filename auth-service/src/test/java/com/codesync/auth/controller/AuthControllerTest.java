package com.codesync.auth.controller;

import com.codesync.auth.dto.*;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.entity.UserStatus;
import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.AdminRequestStatus;
import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.repository.AdminAccessRequestRepository;
import com.codesync.auth.security.JwtUtil;
import com.codesync.auth.service.NotificationClient;
import com.codesync.auth.service.AnalyticsClient;
import com.codesync.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.codesync.auth.exception.BadRequestException;
import com.codesync.auth.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAccessRequestRepository adminAccessRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AnalyticsClient analyticsClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "superAdminEmail", "admin@codesync.com");
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("testuser", "test@test.com", "password123", "Test User", null, null);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("testuser")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .role(UserRole.DEVELOPER)
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendRegistrationEmail(eq("test@test.com"), eq("Test User"));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("testuser", "test@test.com", "password123", "Test User", null, null);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("testuser", "test@test.com", "password123", "Test User", null, null);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("testuser")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .role(UserRole.DEVELOPER)
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build();
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
        
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("hashedPassword")
                .build();
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authController.authenticateUser(request));
    }

    @Test
    void shouldRejectNonExistentUser() {
        LoginRequest request = new LoginRequest("nonexistent@test.com", "password");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authController.authenticateUser(request));
    }

    @Test
    void shouldGetUserProfile() {
        String token = "Bearer jwt-token";
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("testuser")
                .fullName("Test User")
                .role(UserRole.DEVELOPER)
                .status(UserStatus.ACTIVE)
                .build();
        
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId")).thenReturn("1");
        when(jwtUtil.extractAllClaims(token.substring(7))).thenReturn(claims);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authController.getProfile(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturn404WhenProfileNotFound() {
        String token = "Bearer jwt-token";
        
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId")).thenReturn("1");
        when(jwtUtil.extractAllClaims(token.substring(7))).thenReturn(claims);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authController.getProfile(token));
    }

    @Test
    void shouldValidateTokenWithValidToken() {
        String authHeader = "Bearer valid-jwt-token";
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("email")).thenReturn("test@test.com");
        when(claims.get("role")).thenReturn("DEVELOPER");
        when(claims.get("userId")).thenReturn("1");
        
        when(jwtUtil.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtUtil.extractAllClaims("valid-jwt-token")).thenReturn(claims);

        ResponseEntity<?> response = authController.validateToken(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturn400ForInvalidTokenFormat() {
        String authHeader = "invalid-token";
        
        ResponseEntity<?> response = authController.validateToken(authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturn400ForInvalidToken() {
        String authHeader = "Bearer invalid-token";
        
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        ResponseEntity<?> response = authController.validateToken(authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldListAdminRequests() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@codesync.com")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.findByEmail("admin@codesync.com")).thenReturn(Optional.of(adminUser));
        when(adminAccessRequestRepository.findByStatusOrderByCreatedAtDesc(AdminRequestStatus.PENDING))
                .thenReturn(List.of());

        ResponseEntity<?> response = authController.listAdminRequests("admin@codesync.com", AdminRequestStatus.PENDING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}