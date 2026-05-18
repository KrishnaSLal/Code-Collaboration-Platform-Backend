package com.app.authservice.service;

import com.app.authservice.dto.*;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse adminLogin(LoginRequest request);

    AuthResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    AuthResponse resetPassword(ResetPasswordRequest request);

    List<UserSummaryResponse> getUsersByIds(List<Long> userIds);

    UserSummaryResponse getCurrentUser(String authorizationHeader);
}
