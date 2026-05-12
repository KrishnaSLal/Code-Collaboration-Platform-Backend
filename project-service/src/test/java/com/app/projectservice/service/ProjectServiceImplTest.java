package com.app.projectservice.service;

import com.app.projectservice.dto.ProjectRequest;
import com.app.projectservice.dto.ProjectResponse;
import com.app.projectservice.entity.WorkspaceProject;
import com.app.projectservice.exception.ProjectNotFoundException;
import com.app.projectservice.repository.WorkspaceProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private WorkspaceProjectRepository repository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void createProjectPersistsDefaultsAndMapsResponse() {
        ProjectRequest request = ProjectRequest.builder()
                .ownerId(1L)
                .projectName("CodeSync")
                .description("Collaboration platform")
                .language("Java")
                .visibility("PUBLIC")
                .build();

        when(repository.save(any(WorkspaceProject.class))).thenAnswer(invocation -> {
            WorkspaceProject project = invocation.getArgument(0);
            project.setProjectId(100L);
            return project;
        });

        ProjectResponse response = projectService.createProject(request);

        assertThat(response.getProjectId()).isEqualTo(100L);
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getProjectName()).isEqualTo("CodeSync");
        assertThat(response.getArchived()).isFalse();
        assertThat(response.getStarCount()).isZero();
        assertThat(response.getForkCount()).isZero();
    }

    @Test
    void getProjectByIdThrowsWhenProjectDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(404L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project not found with id: 404");
    }

    @Test
    void getPublicProjectsDelegatesToRepositoryAndMapsResults() {
        when(repository.findByVisibilityAndArchivedFalse("PUBLIC"))
                .thenReturn(List.of(project(10L, 1L, "Public Project", "PUBLIC")));

        List<ProjectResponse> responses = projectService.getPublicProjects();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectName()).isEqualTo("Public Project");
        verify(repository).findByVisibilityAndArchivedFalse("PUBLIC");
    }

    @Test
    void getProjectsByOwnerDelegatesToRepositoryAndMapsResults() {
        when(repository.findByOwnerId(1L))
                .thenReturn(List.of(project(10L, 1L, "Owner Project", "PRIVATE")));

        List<ProjectResponse> responses = projectService.getProjectsByOwner(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOwnerId()).isEqualTo(1L);
        assertThat(responses.get(0).getProjectName()).isEqualTo("Owner Project");
    }

    @Test
    void searchProjectsDelegatesToRepositoryAndMapsResults() {
        when(repository.findByProjectNameContainingIgnoreCaseAndArchivedFalse("sync"))
                .thenReturn(List.of(project(10L, 1L, "CodeSync", "PUBLIC")));

        List<ProjectResponse> responses = projectService.searchProjects("sync");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectName()).isEqualTo("CodeSync");
    }

    @Test
    void getProjectsByLanguageDelegatesToRepositoryAndMapsResults() {
        when(repository.findByLanguageAndArchivedFalse("Java"))
                .thenReturn(List.of(project(10L, 1L, "Java Project", "PUBLIC")));

        List<ProjectResponse> responses = projectService.getProjectsByLanguage("Java");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLanguage()).isEqualTo("Java");
    }

    @Test
    void updateProjectUpdatesMutableFields() {
        WorkspaceProject project = project(10L, 1L, "Old", "PRIVATE");
        ProjectRequest request = ProjectRequest.builder()
                .projectName("New")
                .description("New description")
                .language("TypeScript")
                .visibility("PUBLIC")
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(project));
        when(repository.save(any(WorkspaceProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(10L, request);

        assertThat(response.getProjectName()).isEqualTo("New");
        assertThat(response.getDescription()).isEqualTo("New description");
        assertThat(response.getLanguage()).isEqualTo("TypeScript");
        assertThat(response.getVisibility()).isEqualTo("PUBLIC");
    }

    @Test
    void deleteProjectDeletesFetchedProject() {
        WorkspaceProject project = project(10L, 1L, "Delete Me", "PRIVATE");
        when(repository.findById(10L)).thenReturn(Optional.of(project));

        projectService.deleteProject(10L);

        verify(repository).delete(project);
    }

    @Test
    void archiveProjectMarksProjectArchived() {
        WorkspaceProject project = project(10L, 1L, "Archive Me", "PRIVATE");
        when(repository.findById(10L)).thenReturn(Optional.of(project));
        when(repository.save(any(WorkspaceProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.archiveProject(10L);

        assertThat(response.getArchived()).isTrue();
    }

    @Test
    void starProjectIncrementsStarCount() {
        WorkspaceProject project = project(10L, 1L, "Star Me", "PUBLIC");
        project.setStarCount(4);
        when(repository.findById(10L)).thenReturn(Optional.of(project));
        when(repository.save(any(WorkspaceProject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.starProject(10L);

        assertThat(response.getStarCount()).isEqualTo(5);
    }

    @Test
    void forkProjectIncrementsSourceAndCreatesPrivateForkForNewOwner() {
        WorkspaceProject source = project(10L, 1L, "Original", "PUBLIC");
        source.setForkCount(2);
        when(repository.findById(10L)).thenReturn(Optional.of(source));
        when(repository.save(any(WorkspaceProject.class))).thenAnswer(invocation -> {
            WorkspaceProject project = invocation.getArgument(0);
            if (project.getProjectId() == null) {
                project.setProjectId(99L);
            }
            return project;
        });

        ProjectResponse forked = projectService.forkProject(10L, 44L);

        assertThat(source.getForkCount()).isEqualTo(3);
        assertThat(forked.getProjectId()).isEqualTo(99L);
        assertThat(forked.getOwnerId()).isEqualTo(44L);
        assertThat(forked.getProjectName()).isEqualTo("Original-fork");
        assertThat(forked.getVisibility()).isEqualTo("PRIVATE");
        assertThat(forked.getStarCount()).isZero();
        assertThat(forked.getForkCount()).isZero();

        ArgumentCaptor<WorkspaceProject> captor = ArgumentCaptor.forClass(WorkspaceProject.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getForkCount()).isEqualTo(3);
        assertThat(captor.getAllValues().get(1).getOwnerId()).isEqualTo(44L);
    }

    private WorkspaceProject project(Long id, Long ownerId, String name, String visibility) {
        return WorkspaceProject.builder()
                .projectId(id)
                .ownerId(ownerId)
                .projectName(name)
                .description("Description")
                .language("Java")
                .visibility(visibility)
                .archived(false)
                .starCount(0)
                .forkCount(0)
                .build();
    }
}
