package com.app.authservice.service;

import com.app.authservice.dto.AuthResponse;
import com.app.authservice.dto.ForgotPasswordRequest;
import com.app.authservice.dto.LoginRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordRequest;
import com.app.authservice.dto.UserSummaryResponse;
import com.app.authservice.dto.VerifyOtpRequest;
import com.app.authservice.entity.AppUser;
import com.app.authservice.repository.AppUserRepository;
import com.app.authservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesUserWithEncodedPasswordAndToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Krishna");
        request.setEmail("krishna@example.com");
        request.setPassword("plain-password");
        request.setMobileNumber("9999999999");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByMobileNumber(request.getMobileNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(jwtService.generateToken(request.getEmail())).thenReturn("jwt-token");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("krishna@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getMessage()).isEqualTo("User registered successfully!");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getResetOtpVerified()).isFalse();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("krishna@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already registered!");
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("krishna@example.com");
        request.setPassword("wrong");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .password("encoded")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password!");
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("krishna@example.com");
        request.setPassword("plain-password");

        AppUser user = AppUser.builder()
                .id(10L)
                .fullName("Krishna")
                .email("krishna@example.com")
                .password("encoded-password")
                .role("USER")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("krishna@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("krishna@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getMessage()).isEqualTo("Login successful!");
    }

    @Test
    void adminLoginReturnsTokenForAdminUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("plain-password");

        AppUser admin = AppUser.builder()
                .id(1L)
                .fullName("Admin")
                .email("admin@example.com")
                .password("encoded-password")
                .role("ADMIN")
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("admin@example.com")).thenReturn("admin-token");

        AuthResponse response = authService.adminLogin(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getToken()).isEqualTo("admin-token");
        assertThat(response.getMessage()).isEqualTo("Admin login successful!");
    }

    @Test
    void adminLoginRejectsNonAdminUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("plain-password");

        AppUser user = AppUser.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role("USER")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.adminLogin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only platform administrators can use admin login.");
    }

    @Test
    void forgotPasswordStoresOtpAndReturnsFallbackMessageWhenEmailFails() {
        ReflectionTestUtils.setField(authService, "exposeOtp", false);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("krishna@example.com");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .resetOtpVerified(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendOtpEmail(eq("krishna@example.com"), any(String.class))).thenReturn(false);

        AuthResponse response = authService.forgotPassword(request);

        assertThat(user.getResetOtp()).hasSize(6);
        assertThat(user.getResetOtpExpiry()).isAfter(LocalDateTime.now());
        assertThat(user.getResetOtpVerified()).isFalse();
        assertThat(response.getMessage()).contains("OTP email could not be sent");
    }

    @Test
    void verifyOtpRejectsExpiredOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("krishna@example.com");
        request.setOtp("123456");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .resetOtp("123456")
                .resetOtpExpiry(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP expired. Please request a new OTP.");
    }

    @Test
    void verifyOtpMarksOtpAsVerifiedWhenOtpMatches() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("krishna@example.com");
        request.setOtp("123456");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .resetOtp("123456")
                .resetOtpExpiry(LocalDateTime.now().plusMinutes(5))
                .resetOtpVerified(false)
                .build();

        when(userRepository.findByEmail("krishna@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.verifyOtp(request);

        assertThat(user.getResetOtpVerified()).isTrue();
        assertThat(response.getEmail()).isEqualTo("krishna@example.com");
        assertThat(response.getMessage()).isEqualTo("OTP verified successfully.");
    }

    @Test
    void verifyOtpRejectsInvalidOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("krishna@example.com");
        request.setOtp("000000");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .resetOtp("123456")
                .resetOtpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByEmail("krishna@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid OTP!");
    }

    @Test
    void resetPasswordEncodesNewPasswordAndClearsOtpState() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("krishna@example.com");
        request.setNewPassword("new-password");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .password("old-password")
                .resetOtp("123456")
                .resetOtpExpiry(LocalDateTime.now().plusMinutes(5))
                .resetOtpVerified(true)
                .build();

        when(userRepository.findByEmail("krishna@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getResetOtp()).isNull();
        assertThat(user.getResetOtpExpiry()).isNull();
        assertThat(user.getResetOtpVerified()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Password reset successfully.");
    }

    @Test
    void resetPasswordRequiresVerifiedOtp() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("krishna@example.com");
        request.setNewPassword("new-password");

        AppUser user = AppUser.builder()
                .email("krishna@example.com")
                .resetOtpVerified(false)
                .build();

        when(userRepository.findByEmail("krishna@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP verification required before resetting password.");
    }

    @Test
    void getUsersByIdsReturnsRegisteredUserSummaries() {
        AppUser first = AppUser.builder()
                .id(1L)
                .fullName("Krishna")
                .email("krishna@example.com")
                .role("USER")
                .build();
        AppUser second = AppUser.builder()
                .id(2L)
                .fullName("Admin")
                .email("admin@example.com")
                .role("ADMIN")
                .build();

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        List<UserSummaryResponse> users = authService.getUsersByIds(List.of(1L, 2L));

        assertThat(users).hasSize(2);
        assertThat(users)
                .extracting(UserSummaryResponse::getFullName)
                .containsExactly("Krishna", "Admin");
        assertThat(users.get(0).getUserId()).isEqualTo(1L);
        assertThat(users.get(0).getEmail()).isEqualTo("krishna@example.com");
    }
}
