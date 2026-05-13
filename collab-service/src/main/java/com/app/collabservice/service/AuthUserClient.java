package com.app.collabservice.service;

import com.app.collabservice.dto.UserSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuthUserClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${codesync.auth-service.base-url:http://localhost:8081/api/v1/auth}")
    private String authServiceBaseUrl;

    public Map<Long, UserSummaryResponse> getUsersByIds(Collection<Long> userIds) {
        List<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        String url = UriComponentsBuilder.fromHttpUrl(authServiceBaseUrl)
                .path("/users")
                .queryParam("ids", ids.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .toUriString();

        try {
            List<UserSummaryResponse> users = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserSummaryResponse>>() {
                    }
            ).getBody();

            if (users == null) {
                return Map.of();
            }

            return users.stream()
                    .filter(user -> user.getUserId() != null)
                    .collect(Collectors.toMap(UserSummaryResponse::getUserId, user -> user));
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
