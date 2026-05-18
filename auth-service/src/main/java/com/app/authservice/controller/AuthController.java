package com.app.authservice.controller;

import com.app.authservice.dto.*;
import com.app.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody LoginRequest request) {
        return authService.adminLogin(request);
    }

    @PostMapping("/forgot-password")
    public AuthResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/users")
    public List<UserSummaryResponse> getUsersByIds(@RequestParam("ids") List<String> ids) {
        return authService.getUsersByIds(parseUserIds(ids));
    }

    @GetMapping("/me")
    public UserSummaryResponse getCurrentUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return authService.getCurrentUser(authorizationHeader);
    }

    @GetMapping("/test")
    public String test() {
        return "Auth service is working";
    }

    private List<Long> parseUserIds(List<String> ids) {
        List<Long> userIds = new ArrayList<>();
        for (String idGroup : ids) {
            if (idGroup == null || idGroup.isBlank()) {
                continue;
            }

            for (String id : idGroup.split(",")) {
                String trimmedId = id.trim();
                if (!trimmedId.isBlank()) {
                    userIds.add(Long.valueOf(trimmedId));
                }
            }
        }
        return userIds;
    }
}
