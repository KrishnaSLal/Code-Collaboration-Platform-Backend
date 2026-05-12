package com.app.authservice.security;

import com.app.authservice.entity.AppUser;
import com.app.authservice.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauthToken
                ? oauthToken.getAuthorizedClientRegistrationId()
                : "oauth";
        String email = oauthUser.getAttribute("email");
        if ((email == null || email.isBlank()) && authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = resolveGithubEmail(oauthToken);
        }
        String name = resolveName(oauthUser, email);
        String subject = resolveSubject(oauthUser, registrationId);

        if (email == null || email.isBlank()) {
            throw new ServletException("OAuth provider did not provide an email address.");
        }

        String resolvedEmail = email;
        String resolvedName = name == null || name.isBlank() ? resolvedEmail : name;
        String resolvedMobileNumber = registrationId.toUpperCase() + "-" + subject;

        AppUser user = userRepository.findByEmail(resolvedEmail)
                .orElseGet(() -> userRepository.save(AppUser.builder()
                        .fullName(resolvedName)
                        .email(resolvedEmail)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .mobileNumber(resolvedMobileNumber)
                        .role("USER")
                        .resetOtpVerified(false)
                        .build()));

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getEmail());
        String redirectUrl = "http://localhost:4200/oauth-success"
                + "?token=" + encode(token)
                + "&userId=" + user.getId()
                + "&fullName=" + encode(user.getFullName())
                + "&email=" + encode(user.getEmail());

        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String resolveName(OAuth2User oauthUser, String email) {
        String name = oauthUser.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String login = oauthUser.getAttribute("login");
        return login == null || login.isBlank() ? email : login;
    }

    private String resolveSubject(OAuth2User oauthUser, String registrationId) {
        Object providerId = oauthUser.getAttribute("sub");
        if (providerId == null) {
            providerId = oauthUser.getAttribute("id");
        }

        return providerId == null ? registrationId + "-" + UUID.randomUUID() : providerId.toString();
    }

    private String resolveGithubEmail(OAuth2AuthenticationToken oauthToken) {
        if (!"github".equalsIgnoreCase(oauthToken.getAuthorizedClientRegistrationId())) {
            return null;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(client.getAccessToken().getTokenValue());
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<List<Map<String, Object>>> response = new RestTemplate().exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> emails = response.getBody();
        if (emails == null) {
            return null;
        }

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                .map(email -> String.valueOf(email.get("email")))
                .findFirst()
                .orElse(null);
    }
}
