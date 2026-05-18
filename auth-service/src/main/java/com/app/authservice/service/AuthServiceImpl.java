package com.app.authservice.service;

import com.app.authservice.dto.*;
import com.app.authservice.entity.AppUser;
import com.app.authservice.repository.AppUserRepository;
import com.app.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${codesync.auth.expose-otp:false}")
    private boolean exposeOtp;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered!");
        }

        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new RuntimeException("Mobile number already registered!");
        }

        AppUser user = AppUser.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .mobileNumber(request.getMobileNumber())
                .role("USER")
                .resetOtpVerified(false)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("User registered successfully!")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password!");
        }

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("Login successful!")
                .build();
    }

    @Override
    public AuthResponse adminLogin(LoginRequest request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid admin email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid admin email or password!");
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Only platform administrators can use admin login.");
        }

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("Admin login successful!")
                .build();
    }

    @Override
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not registered!"));

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setResetOtpVerified(false);

        userRepository.save(user);

        boolean emailSent = emailService.sendOtpEmail(user.getEmail(), otp);

        String message = emailSent
                ? "OTP sent successfully to your email."
                : "OTP email could not be sent. Use this development OTP: " + otp;

        if (emailSent && exposeOtp) {
            message = message + " Development OTP: " + otp;
        }

        return AuthResponse.builder()
                .email(user.getEmail())
                .message(message)
                .build();
    }

    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not registered!"));

        if (user.getResetOtp() == null) {
            throw new RuntimeException("OTP not found. Please request a new OTP.");
        }

        if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new OTP.");
        }

        if (!user.getResetOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP!");
        }

        user.setResetOtpVerified(true);
        userRepository.save(user);

        return AuthResponse.builder()
                .email(user.getEmail())
                .message("OTP verified successfully.")
                .build();
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not registered!"));

        if (user.getResetOtpVerified() == null || !user.getResetOtpVerified()) {
            throw new RuntimeException("OTP verification required before resetting password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        user.setResetOtpVerified(false);

        userRepository.save(user);

        return AuthResponse.builder()
                .email(user.getEmail())
                .message("Password reset successfully.")
                .build();
    }

    @Override
    public List<UserSummaryResponse> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds)
                .stream()
                .map(this::mapUserSummary)
                .collect(Collectors.toList());
    }

    @Override
    public UserSummaryResponse getCurrentUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (!jwtService.isTokenValid(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(token);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return mapUserSummary(user);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authentication required");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new RuntimeException("Authentication required");
        }
        return token;
    }

    private UserSummaryResponse mapUserSummary(AppUser user) {
        return UserSummaryResponse.builder()
                .userId(user.getId())
                .username(user.getFullName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
