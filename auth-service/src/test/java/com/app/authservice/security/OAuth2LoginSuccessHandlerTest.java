package com.app.authservice.security;

import com.app.authservice.entity.AppUser;
import com.app.authservice.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(userRepository, jwtService, passwordEncoder, authorizedClientService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void createsUserAndRedirectsWithToken() throws Exception {
        OAuth2User oauthUser = oauthUser(Map.of(
                "sub", "google-123",
                "email", "krishna@example.com",
                "name", "Krishna"
        ), "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauthUser,
                oauthUser.getAuthorities(),
                "google"
        );

        when(userRepository.findByEmail("krishna@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(jwtService.generateToken("krishna@example.com")).thenReturn("jwt token");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo("Krishna");
        assertThat(savedUser.getEmail()).isEqualTo("krishna@example.com");
        assertThat(savedUser.getMobileNumber()).isEqualTo("GOOGLE-google-123");
        assertThat(savedUser.getRole()).isEqualTo("USER");
        assertThat(savedUser.getResetOtpVerified()).isFalse();

        verify(response).sendRedirect("http://localhost:4200/oauth-success"
                + "?token=jwt+token"
                + "&userId=7"
                + "&fullName=Krishna"
                + "&email=krishna%40example.com");
    }

    @Test
    void existingUserWithBlankRoleIsNormalizedBeforeRedirect() throws Exception {
        AppUser user = AppUser.builder()
                .id(8L)
                .fullName("Existing User")
                .email("existing@example.com")
                .password("encoded")
                .mobileNumber("GOOGLE-existing")
                .role(" ")
                .build();
        OAuth2User oauthUser = oauthUser(Map.of(
                "sub", "existing",
                "email", "existing@example.com",
                "login", "fallback-login"
        ), "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauthUser,
                oauthUser.getAuthorities(),
                "google"
        );

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("existing@example.com")).thenReturn("existing-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(user.getRole()).isEqualTo("USER");
        verify(userRepository).save(user);
        verify(response).sendRedirect("http://localhost:4200/oauth-success"
                + "?token=existing-token"
                + "&userId=8"
                + "&fullName=Existing+User"
                + "&email=existing%40example.com");
    }

    @Test
    void missingEmailFromNonGithubProviderThrowsServletException() {
        OAuth2User oauthUser = oauthUser(Map.of("sub", "provider-123"), "sub");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(oauthUser, null);

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(ServletException.class)
                .hasMessage("OAuth provider did not provide an email address.");
    }

    @Test
    void githubAuthenticationWithoutAuthorizedClientThrowsServletException() {
        OAuth2User oauthUser = oauthUser(Map.of("id", 12345), "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauthUser,
                oauthUser.getAuthorities(),
                "github"
        );
        when(authorizedClientService.loadAuthorizedClient("github", "12345")).thenReturn(null);

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(ServletException.class)
                .hasMessage("OAuth provider did not provide an email address.");
    }

    private OAuth2User oauthUser(Map<String, Object> attributes, String nameAttributeKey) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                nameAttributeKey
        );
    }
}
