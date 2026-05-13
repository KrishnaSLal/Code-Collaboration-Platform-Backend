package com.app.executionservice.controller;

import com.app.executionservice.dto.ExecutionRequest;
import com.app.executionservice.dto.ExecutionResponse;
import com.app.executionservice.dto.ExecutionStatsResponse;
import com.app.executionservice.dto.LanguageInfoResponse;
import com.app.executionservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping
    public ExecutionResponse submitExecution(@RequestBody ExecutionRequest request) {
        return executionService.submitExecution(request);
    }

    @GetMapping("/{jobId}")
    public ExecutionResponse getJobById(@PathVariable String jobId) {
        return executionService.getJobById(jobId);
    }

    @GetMapping("/user/{userId}")
    public List<ExecutionResponse> getExecutionsByUser(@PathVariable Long userId) {
        return executionService.getExecutionsByUser(userId);
    }

    @GetMapping("/project/{projectId}")
    public List<ExecutionResponse> getExecutionsByProject(@PathVariable Long projectId) {
        return executionService.getExecutionsByProject(projectId);
    }

    @PostMapping("/{jobId}/cancel")
    public ExecutionResponse cancelExecution(@PathVariable String jobId) {
        return executionService.cancelExecution(jobId);
    }

    @GetMapping("/{jobId}/result")
    public ExecutionResponse getExecutionResult(@PathVariable String jobId) {
        return executionService.getExecutionResult(jobId);
    }

    @GetMapping("/supportedLanguages")
    public List<LanguageInfoResponse> getSupportedLanguages() {
        return executionService.getSupportedLanguages();
    }

    @GetMapping("/languageVersion/{language}")
    public LanguageInfoResponse getLanguageVersion(@PathVariable String language) {
        return executionService.getLanguageVersion(language);
    }

    @GetMapping("/stats")
    public ExecutionStatsResponse getExecutionStats() {
        return executionService.getExecutionStats();
    }

    @GetMapping("/stats/user/{userId}")
    public ExecutionStatsResponse getExecutionStatsByUser(@PathVariable Long userId) {
        return executionService.getExecutionStatsByUser(userId);
    }

    @DeleteMapping("/project/{projectId}")
    public String deleteExecutionsByProject(@PathVariable Long projectId) {
        executionService.deleteExecutionsByProject(projectId);
        return "Project executions deleted successfully";
    }
}
