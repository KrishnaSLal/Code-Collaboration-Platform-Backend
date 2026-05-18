package com.app.collabservice.service;

import com.app.collabservice.dto.ProjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProjectClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${codesync.project-service.base-url:http://localhost:8082/projects}")
    private String projectServiceBaseUrl;

    public ProjectResponse getProjectById(Long projectId) {
        if (projectId == null) {
            throw new RuntimeException("Project id is required");
        }

        String url = UriComponentsBuilder.fromHttpUrl(projectServiceBaseUrl)
                .path("/{projectId}")
                .buildAndExpand(projectId)
                .toUriString();

        try {
            ProjectResponse project = restTemplate.getForObject(url, ProjectResponse.class);
            if (project == null) {
                throw new RuntimeException("Project not found with id: " + projectId);
            }
            return project;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new RuntimeException("Project not found with id: " + projectId, exception);
        } catch (RestClientException exception) {
            throw new RuntimeException("Unable to verify project ownership", exception);
        }
    }
}
