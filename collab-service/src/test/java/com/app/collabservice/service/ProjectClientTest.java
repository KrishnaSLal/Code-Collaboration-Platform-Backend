package com.app.collabservice.service;

import com.app.collabservice.dto.ProjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProjectClientTest {

    private ProjectClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new ProjectClient();
        ReflectionTestUtils.setField(client, "projectServiceBaseUrl", "http://project.test/projects");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void returnsProjectById() {
        server.expect(requestTo("http://project.test/projects/1"))
                .andRespond(withSuccess("""
                        {"projectId":1,"ownerId":3}
                        """, MediaType.APPLICATION_JSON));

        ProjectResponse response = client.getProjectById(1L);

        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getOwnerId()).isEqualTo(3L);
        server.verify();
    }

    @Test
    void rejectsMissingProjectIdBeforeCallingProjectService() {
        assertThatThrownBy(() -> client.getProjectById(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project id is required");
    }

    @Test
    void throwsProjectNotFoundForNotFoundResponse() {
        server.expect(requestTo("http://project.test/projects/404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getProjectById(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found with id: 404");
        server.verify();
    }

    @Test
    void throwsVerificationFailureWhenProjectServiceCallFails() {
        server.expect(requestTo("http://project.test/projects/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getProjectById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to verify project ownership");
        server.verify();
    }
}
