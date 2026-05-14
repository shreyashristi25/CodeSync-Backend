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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, emailService, "admin@test.com");
    }

    @Test
    void shouldFindById() {
        User user = User.builder().id(1L).email("test@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = authService.findById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldReturnNullWhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        User result = authService.findById(1L);

        assertNull(result);
    }

    @Test
    void shouldFindByRazorpaySubscriptionId() {
        User user = User.builder().id(1L).razorpaySubscriptionId("sub_123").build();
        when(userRepository.findByRazorpaySubscriptionId("sub_123")).thenReturn(Optional.of(user));

        User result = authService.findByRazorpaySubscriptionId("sub_123");

        assertEquals("sub_123", result.getRazorpaySubscriptionId());
    }

    @Test
    void shouldSaveUser() {
        User user = User.builder().email("test@test.com").build();
        when(userRepository.save(user)).thenReturn(user);

        User result = authService.saveUser(user);

        assertNotNull(result);
    }

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest request = new RegisterRequest("test@test.com", "testuser", "Test User", "password123", "avatar", "bio");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("token123");

        AuthResponse result = authService.register(request);

        assertNotNull(result);
        assertEquals("test@test.com", result.email());
        assertEquals("testuser", result.username());
        verify(emailService).sendRegistrationEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("existing@test.com", "existinguser", "Test", "password", null, null);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void shouldThrowWhenUsernameAlreadyTaken() {
        RegisterRequest request = new RegisterRequest("new@test.com", "newuser", "Test", "password", null, null);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

@Test
    void shouldAssignAdminRoleForSuperAdminEmail() {
        AuthService adminService = new AuthService(userRepository, passwordEncoder, jwtUtil, emailService, "admin@test.com");
        RegisterRequest request = new RegisterRequest("admin@test.com", "adminuser", "Admin", "password123", null, null);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("token");

        AuthResponse result = adminService.register(request);

        assertEquals("ADMIN", result.role());
    }

    @Test
    void shouldLoginSuccessfully() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("test")
                .fullName("Test User")
                .passwordHash("encoded")
                .role(UserRole.DEVELOPER)
                .provider(AuthProvider.LOCAL)
                .build();
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("token123");

        AuthResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("test@test.com", result.email());
        verify(emailService).sendLoginNotificationEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        LoginRequest request = new LoginRequest("notfound@test.com", "password");
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void shouldThrowWhenPasswordInvalid() {
        User user = User.builder().email("test@test.com").passwordHash("encoded").build();
        LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void shouldBuildOAuthResponse() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("test")
                .fullName("Test")
                .role(UserRole.DEVELOPER)
                .provider(AuthProvider.GITHUB)
                .build();
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("token");

        AuthResponse result = authService.buildOAuthResponse(user);

        assertNotNull(result);
        assertEquals("test@test.com", result.email());
    }

    @Test
    void shouldSendPasswordResetEmail() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .fullName("Test")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.forgotPassword("test@test.com");

        verify(userRepository).save(any(User.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldNotSendEmailForNonLocalProvider() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .provider(AuthProvider.GITHUB)
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("test@test.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void shouldResetPasswordSuccessfully() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordResetToken("token123")
                .passwordResetExpiry(java.time.LocalDateTime.now().plusMinutes(30))
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword("test@test.com", "token123", "newpassword");

        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetExpiry());
    }

    @Test
    void shouldThrowOnInvalidResetToken() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordResetToken("token123")
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> 
            authService.resetPassword("test@test.com", "wrongtoken", "newpassword"));
    }

    @Test
    void shouldThrowOnExpiredResetToken() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordResetToken("token123")
                .passwordResetExpiry(java.time.LocalDateTime.now().minusMinutes(10))
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> 
            authService.resetPassword("test@test.com", "token123", "newpassword"));
    }
}