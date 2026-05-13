package com.app.executionservice.controller;

import com.app.executionservice.dto.ExecutionRequest;
import com.app.executionservice.dto.ExecutionResponse;
import com.app.executionservice.dto.ExecutionStatsResponse;
import com.app.executionservice.dto.LanguageInfoResponse;
import com.app.executionservice.service.ExecutionService;
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
class ExecutionControllerTest {

    @Mock
    private ExecutionService executionService;

    private ExecutionController controller;

    @BeforeEach
    void setUp() {
        controller = new ExecutionController(executionService);
    }

    @Test
    void delegatesExecutionCommandsAndQueries() {
        ExecutionRequest request = ExecutionRequest.builder().language("Python").build();
        ExecutionResponse response = ExecutionResponse.builder().jobId("job-1").status("COMPLETED").build();
        ExecutionStatsResponse stats = ExecutionStatsResponse.builder().totalExecutions(3L).build();
        LanguageInfoResponse language = new LanguageInfoResponse("Python", "3.x");

        when(executionService.submitExecution(request)).thenReturn(response);
        when(executionService.getJobById("job-1")).thenReturn(response);
        when(executionService.getExecutionsByUser(7L)).thenReturn(List.of(response));
        when(executionService.getExecutionsByProject(9L)).thenReturn(List.of(response));
        when(executionService.cancelExecution("job-1")).thenReturn(response);
        when(executionService.getExecutionResult("job-1")).thenReturn(response);
        when(executionService.getSupportedLanguages()).thenReturn(List.of(language));
        when(executionService.getLanguageVersion("python")).thenReturn(language);
        when(executionService.getExecutionStats()).thenReturn(stats);
        when(executionService.getExecutionStatsByUser(7L)).thenReturn(stats);

        assertThat(controller.submitExecution(request)).isSameAs(response);
        assertThat(controller.getJobById("job-1")).isSameAs(response);
        assertThat(controller.getExecutionsByUser(7L)).containsExactly(response);
        assertThat(controller.getExecutionsByProject(9L)).containsExactly(response);
        assertThat(controller.cancelExecution("job-1")).isSameAs(response);
        assertThat(controller.getExecutionResult("job-1")).isSameAs(response);
        assertThat(controller.getSupportedLanguages()).containsExactly(language);
        assertThat(controller.getLanguageVersion("python")).isSameAs(language);
        assertThat(controller.getExecutionStats()).isSameAs(stats);
        assertThat(controller.getExecutionStatsByUser(7L)).isSameAs(stats);
        assertThat(controller.deleteExecutionsByProject(9L)).isEqualTo("Project executions deleted successfully");

        verify(executionService).deleteExecutionsByProject(9L);
    }
}
