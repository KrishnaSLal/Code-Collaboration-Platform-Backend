package com.app.authservice.controller;

import com.app.authservice.dto.UserSummaryResponse;
import com.app.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void getUsersByIdsAcceptsCommaSeparatedAndRepeatedIds() {
        List<UserSummaryResponse> users = List.of(UserSummaryResponse.builder()
                .userId(8L)
                .username("Shweta")
                .fullName("Shweta")
                .email("shweta@example.com")
                .build());

        when(authService.getUsersByIds(List.of(8L, 1L, 2L))).thenReturn(users);

        List<UserSummaryResponse> response = authController.getUsersByIds(List.of("8,1", "2"));

        assertThat(response).isEqualTo(users);
        verify(authService).getUsersByIds(List.of(8L, 1L, 2L));
    }
}
