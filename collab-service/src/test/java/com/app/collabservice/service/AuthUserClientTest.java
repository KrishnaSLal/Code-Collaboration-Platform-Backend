package com.app.collabservice.service;

import com.app.collabservice.dto.UserSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthUserClientTest {

    private AuthUserClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new AuthUserClient();
        ReflectionTestUtils.setField(client, "authServiceBaseUrl", "http://auth.test/api/v1/auth");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void returnsEmptyMapForEmptyIds() {
        assertThat(client.getUsersByIds(Arrays.asList(null, null))).isEmpty();
    }

    @Test
    void returnsUsersByIdAndFiltersNullUserIds() {
        server.expect(requestTo("http://auth.test/api/v1/auth/users?ids=1,2"))
                .andRespond(withSuccess("""
                        [
                          {"userId":1,"username":"krishna","fullName":"Krishna","email":"krishna@example.com"},
                          {"userId":null,"username":"ignored"},
                          {"userId":2,"username":"admin","fullName":"Admin","email":"admin@example.com"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        Map<Long, UserSummaryResponse> users = client.getUsersByIds(Arrays.asList(1L, 2L, 1L, null));

        assertThat(users).containsOnlyKeys(1L, 2L);
        assertThat(users.get(1L).getFullName()).isEqualTo("Krishna");
        assertThat(users.get(2L).getEmail()).isEqualTo("admin@example.com");
        server.verify();
    }

    @Test
    void returnsEmptyMapForNullResponseBody() {
        server.expect(requestTo("http://auth.test/api/v1/auth/users?ids=1"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThat(client.getUsersByIds(List.of(1L))).isEmpty();
    }

    @Test
    void returnsEmptyMapWhenAuthServiceCallFails() {
        server.expect(requestTo("http://auth.test/api/v1/auth/users?ids=1"))
                .andRespond(withServerError());

        assertThat(client.getUsersByIds(List.of(1L))).isEmpty();
    }
}
