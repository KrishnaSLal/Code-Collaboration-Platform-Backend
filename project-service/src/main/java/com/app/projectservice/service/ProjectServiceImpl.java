package com.app.projectservice.service;

import com.app.projectservice.dto.ProjectRequest;
import com.app.projectservice.dto.ProjectResponse;
import com.app.projectservice.entity.WorkspaceProject;
import com.app.projectservice.exception.ProjectNotFoundException;
import com.app.projectservice.repository.WorkspaceProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final WorkspaceProjectRepository repository;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        WorkspaceProject project = WorkspaceProject.builder()
                .ownerId(request.getOwnerId())
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .language(request.getLanguage())
                .visibility(request.getVisibility())
                .archived(false)
                .starCount(0)
                .forkCount(0)
                .build();

        return mapToResponse(repository.save(project));
    }

    @Override
    public ProjectResponse getProjectById(Long projectId) {
        WorkspaceProject project = getProject(projectId);
        return mapToResponse(project);
    }

    @Override
    public List<ProjectResponse> getProjectsByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getPublicProjects() {
        return repository.findByVisibilityAndArchivedFalse("PUBLIC")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> searchProjects(String keyword) {
        return repository.findByProjectNameContainingIgnoreCaseAndArchivedFalse(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getProjectsByLanguage(String language) {
        return repository.findByLanguageAndArchivedFalse(language)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {

        WorkspaceProject project = getProject(projectId);

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setLanguage(request.getLanguage());
        project.setVisibility(request.getVisibility());

        return mapToResponse(repository.save(project));
    }

    @Override
    public void deleteProject(Long projectId) {
        repository.delete(getProject(projectId));
    }

    @Override
    public ProjectResponse archiveProject(Long projectId) {
        WorkspaceProject project = getProject(projectId);
        project.setArchived(true);
        return mapToResponse(repository.save(project));
    }

    @Override
    public ProjectResponse starProject(Long projectId) {
        WorkspaceProject project = getProject(projectId);
        project.setStarCount(project.getStarCount() + 1);
        return mapToResponse(repository.save(project));
    }

    @Override
    public ProjectResponse forkProject(Long projectId, Long newOwnerId) {

        WorkspaceProject source = getProject(projectId);

        source.setForkCount(source.getForkCount() + 1);
        repository.save(source);

        WorkspaceProject forked = WorkspaceProject.builder()
                .ownerId(newOwnerId)
                .projectName(source.getProjectName() + "-fork")
                .description(source.getDescription())
                .language(source.getLanguage())
                .visibility("PRIVATE")
                .archived(false)
                .starCount(0)
                .forkCount(0)
                .build();

        return mapToResponse(repository.save(forked));
    }

    private WorkspaceProject getProject(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with id: " + id));
    }

    private ProjectResponse mapToResponse(WorkspaceProject project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .ownerId(project.getOwnerId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .language(project.getLanguage())
                .visibility(project.getVisibility())
                .archived(project.getArchived())
                .starCount(project.getStarCount())
                .forkCount(project.getForkCount())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}