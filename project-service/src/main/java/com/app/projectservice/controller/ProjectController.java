package com.app.projectservice.controller;

import com.app.projectservice.dto.ProjectRequest;
import com.app.projectservice.dto.ProjectResponse;
import com.app.projectservice.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ProjectResponse createProject(@RequestBody ProjectRequest request) {
        return service.createProject(request);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProjectById(@PathVariable Long projectId) {
        return service.getProjectById(projectId);
    }

    @GetMapping("/owner/{ownerId}")
    public List<ProjectResponse> getProjectsByOwner(@PathVariable Long ownerId) {
        return service.getProjectsByOwner(ownerId);
    }

    @GetMapping("/public")
    public List<ProjectResponse> getPublicProjects() {
        return service.getPublicProjects();
    }

    @GetMapping("/search")
    public List<ProjectResponse> searchProjects(@RequestParam String keyword) {
        return service.searchProjects(keyword);
    }

    @GetMapping("/language/{language}")
    public List<ProjectResponse> getProjectsByLanguage(@PathVariable String language) {
        return service.getProjectsByLanguage(language);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse updateProject(@PathVariable Long projectId,
                                         @RequestBody ProjectRequest request) {
        return service.updateProject(projectId, request);
    }

    @PutMapping("/{projectId}/archive")
    public ProjectResponse archiveProject(@PathVariable Long projectId) {
        return service.archiveProject(projectId);
    }

    @PutMapping("/{projectId}/star")
    public ProjectResponse starProject(@PathVariable Long projectId) {
        return service.starProject(projectId);
    }

    @PostMapping("/{projectId}/fork/{newOwnerId}")
    public ProjectResponse forkProject(@PathVariable Long projectId,
                                       @PathVariable Long newOwnerId) {
        return service.forkProject(projectId, newOwnerId);
    }

    @DeleteMapping("/{projectId}")
    public String deleteProject(@PathVariable Long projectId) {
        service.deleteProject(projectId);
        return "Project deleted successfully";
    }
}