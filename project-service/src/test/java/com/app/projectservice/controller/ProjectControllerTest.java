package com.app.projectservice.controller;

import com.app.projectservice.dto.ProjectRequest;
import com.app.projectservice.dto.ProjectResponse;
import com.app.projectservice.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService service;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(service);
    }

    @Test
    void delegatesProjectOperations() {
        ProjectRequest request = ProjectRequest.builder().projectName("CodeSync").build();
        ProjectResponse response = ProjectResponse.builder().projectId(1L).projectName("CodeSync").build();

        when(service.createProject(request)).thenReturn(response);
        when(service.getProjectById(1L)).thenReturn(response);
        when(service.getProjectsByOwner(7L)).thenReturn(List.of(response));
        when(service.getPublicProjects()).thenReturn(List.of(response));
        when(service.searchProjects("sync")).thenReturn(List.of(response));
        when(service.getProjectsByLanguage("Java")).thenReturn(List.of(response));
        when(service.updateProject(1L, request)).thenReturn(response);
        when(service.archiveProject(1L)).thenReturn(response);
        when(service.starProject(1L)).thenReturn(response);
        when(service.forkProject(1L, 8L)).thenReturn(response);

        assertThat(controller.createProject(request)).isSameAs(response);
        assertThat(controller.getProjectById(1L)).isSameAs(response);
        assertThat(controller.getProjectsByOwner(7L)).containsExactly(response);
        assertThat(controller.getPublicProjects()).containsExactly(response);
        assertThat(controller.searchProjects("sync")).containsExactly(response);
        assertThat(controller.getProjectsByLanguage("Java")).containsExactly(response);
        assertThat(controller.updateProject(1L, request)).isSameAs(response);
        assertThat(controller.archiveProject(1L)).isSameAs(response);
        assertThat(controller.starProject(1L)).isSameAs(response);
        assertThat(controller.forkProject(1L, 8L)).isSameAs(response);
        assertThat(controller.deleteProject(1L)).isEqualTo("Project deleted successfully");

        verify(service).deleteProject(1L);
    }
}
