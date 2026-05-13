package com.app.executionservice.service;

import com.app.executionservice.dto.ExecutionRequest;
import com.app.executionservice.dto.ExecutionResponse;
import com.app.executionservice.dto.ExecutionStatsResponse;
import com.app.executionservice.dto.LanguageInfoResponse;

import java.util.List;

public interface ExecutionService {

    ExecutionResponse submitExecution(ExecutionRequest request);

    ExecutionResponse getJobById(String jobId);

    List<ExecutionResponse> getExecutionsByUser(Long userId);

    List<ExecutionResponse> getExecutionsByProject(Long projectId);

    ExecutionResponse cancelExecution(String jobId);

    ExecutionResponse getExecutionResult(String jobId);

    List<LanguageInfoResponse> getSupportedLanguages();

    LanguageInfoResponse getLanguageVersion(String language);

    ExecutionStatsResponse getExecutionStats();

    ExecutionStatsResponse getExecutionStatsByUser(Long userId);

    void deleteExecutionsByProject(Long projectId);
}
