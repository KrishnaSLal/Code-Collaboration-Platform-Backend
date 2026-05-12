package com.app.projectservice.service;

import com.app.projectservice.dto.ProjectRequest;
import com.app.projectservice.dto.ProjectResponse;

import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse getProjectById(Long projectId);

    List<ProjectResponse> getProjectsByOwner(Long ownerId);

    List<ProjectResponse> getPublicProjects();

    List<ProjectResponse> searchProjects(String keyword);

    List<ProjectResponse> getProjectsByLanguage(String language);

    ProjectResponse updateProject(Long projectId, ProjectRequest request);

    void deleteProject(Long projectId);

    ProjectResponse archiveProject(Long projectId);

    ProjectResponse starProject(Long projectId);

    ProjectResponse forkProject(Long projectId, Long newOwnerId);
}