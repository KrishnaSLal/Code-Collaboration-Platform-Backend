package com.app.executionservice.service;

import com.app.executionservice.dto.ExecutionRequest;
import com.app.executionservice.dto.ExecutionResponse;
import com.app.executionservice.dto.ExecutionStatsResponse;
import com.app.executionservice.dto.LanguageInfoResponse;
import com.app.executionservice.entity.ExecutionJob;
import com.app.executionservice.exception.ExecutionJobNotFoundException;
import com.app.executionservice.repository.ExecutionJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

    @Mock
    private ExecutionJobRepository repository;

    @InjectMocks
    private ExecutionServiceImpl executionService;

    @Test
    void getSupportedLanguagesIncludesExpectedRuntimes() {
        List<LanguageInfoResponse> languages = executionService.getSupportedLanguages();

        assertThat(languages)
                .extracting(LanguageInfoResponse::getLanguage)
                .contains("Python", "Java", "JavaScript", "C++", "TypeScript");
    }

    @Test
    void getLanguageVersionMatchesIgnoringCase() {
        LanguageInfoResponse response = executionService.getLanguageVersion("python");

        assertThat(response.getLanguage()).isEqualTo("Python");
        assertThat(response.getVersion()).isEqualTo("3.x");
    }

    @Test
    void getLanguageVersionThrowsForUnsupportedLanguage() {
        assertThatThrownBy(() -> executionService.getLanguageVersion("Elixir"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Language not supported: Elixir");
    }

    @Test
    void submitExecutionPersistsFailedJobForUnsupportedLanguage() {
        when(repository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutionResponse response = executionService.submitExecution(ExecutionRequest.builder()
                .projectId(1L)
                .fileId(2L)
                .userId(3L)
                .language("Elixir")
                .sourceCode("IO.puts(\"hello\")")
                .stdin("")
                .build());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getExitCode()).isEqualTo(-1);
        assertThat(response.getStderr()).isEqualTo("Language is not runnable by this service: Elixir");
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    void getExecutionsByUserReturnsMappedJobs() {
        when(repository.findByUserId(3L)).thenReturn(List.of(job("job-1", "COMPLETED")));

        List<ExecutionResponse> responses = executionService.getExecutionsByUser(3L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(3L);
    }

    @Test
    void getExecutionsByProjectReturnsMappedJobs() {
        when(repository.findByProjectId(1L)).thenReturn(List.of(job("job-1", "COMPLETED")));

        List<ExecutionResponse> responses = executionService.getExecutionsByProject(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void getExecutionResultReturnsFetchedJob() {
        when(repository.findByJobId("job-1")).thenReturn(Optional.of(job("job-1", "COMPLETED")));

        ExecutionResponse response = executionService.getExecutionResult("job-1");

        assertThat(response.getJobId()).isEqualTo("job-1");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void cancelExecutionMarksQueuedJobAsCancelled() {
        ExecutionJob job = job("job-1", "QUEUED");
        when(repository.findByJobId("job-1")).thenReturn(Optional.of(job));
        when(repository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutionResponse response = executionService.cancelExecution("job-1");

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getCompletedAt()).isNotNull();
        verify(repository).save(job);
    }

    @Test
    void cancelExecutionRejectsFinishedJob() {
        when(repository.findByJobId("job-1")).thenReturn(Optional.of(job("job-1", "COMPLETED")));

        assertThatThrownBy(() -> executionService.cancelExecution("job-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot cancel a finished execution job");
    }

    @Test
    void getJobByIdThrowsWhenMissing() {
        when(repository.findByJobId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> executionService.getJobById("missing"))
                .isInstanceOf(ExecutionJobNotFoundException.class)
                .hasMessage("Execution job not found with id: missing");
    }

    @Test
    void getExecutionStatsAggregatesStatusCounts() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByStatus("COMPLETED")).thenReturn(6L);
        when(repository.countByStatus("FAILED")).thenReturn(3L);
        when(repository.countByStatus("CANCELLED")).thenReturn(1L);

        ExecutionStatsResponse response = executionService.getExecutionStats();

        assertThat(response.getTotalExecutions()).isEqualTo(10L);
        assertThat(response.getCompletedExecutions()).isEqualTo(6L);
        assertThat(response.getFailedExecutions()).isEqualTo(3L);
        assertThat(response.getCancelledExecutions()).isEqualTo(1L);
    }

    @Test
    void getExecutionStatsByUserAggregatesUserStatusCounts() {
        when(repository.countByUserId(3L)).thenReturn(8L);
        when(repository.countByUserIdAndStatus(3L, "COMPLETED")).thenReturn(5L);
        when(repository.countByUserIdAndStatus(3L, "FAILED")).thenReturn(2L);
        when(repository.countByUserIdAndStatus(3L, "CANCELLED")).thenReturn(1L);

        ExecutionStatsResponse response = executionService.getExecutionStatsByUser(3L);

        assertThat(response.getTotalExecutions()).isEqualTo(8L);
        assertThat(response.getCompletedExecutions()).isEqualTo(5L);
        assertThat(response.getFailedExecutions()).isEqualTo(2L);
        assertThat(response.getCancelledExecutions()).isEqualTo(1L);
    }

    @Test
    void deleteExecutionsByProjectDelegatesToRepository() {
        executionService.deleteExecutionsByProject(42L);

        verify(repository).deleteByProjectId(42L);
    }

    @Test
    void createProcessPlanWritesRunnableSourceFilesForSupportedLanguages() throws Exception {
        Path workDir = Files.createTempDirectory("codesync-plan-test-");
        try {
            assertPlan("JavaScript", "Main.js", List.of("node", "Main.js"), workDir);
            assertPlan("node", "Main.js", List.of("node", "Main.js"), workDir);
            assertPlan("TypeScript", "Main.ts", List.of("npx", "tsx", "Main.ts"), workDir);
            assertPlan("Java", "Main.java", shellCommand("javac Main.java && java Main"), workDir);
            assertPlan("C", "main.c", shellCommand("gcc main.c -o main && ./main", "gcc main.c -o main.exe && main.exe"), workDir);
            assertPlan("cpp", "main.cpp", shellCommand("g++ main.cpp -o main && ./main", "g++ main.cpp -o main.exe && main.exe"), workDir);
        } finally {
            ReflectionTestUtils.invokeMethod(executionService, "deleteDirectory", workDir);
        }
    }

    @Test
    void privateHelpersHandleSimpleBranches() {
        String output = ReflectionTestUtils.invokeMethod(executionService, "readProcessOutput",
                "hello".getBytes(StandardCharsets.UTF_8));
        assertThat(output).isEqualTo("hello");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(executionService, "isRunnableExecutable", (String) null))
                .isFalse();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(executionService, "isRunnableExecutable", " "))
                .isFalse();

        ProcessBuilder builder = new ProcessBuilder(isWindows() ? "cmd" : "/bin/sh");
        ReflectionTestUtils.invokeMethod(executionService, "configurePythonEnvironment", builder, (String) null);
        ReflectionTestUtils.invokeMethod(executionService, "configurePythonEnvironment", builder, "node.exe");

        assertThat(builder.environment()).doesNotContainKey("PYTHONHOME");
    }

    @Test
    void deleteDirectoryIgnoresMissingDirectoryAndDeletesExistingTree() throws Exception {
        Path missing = Files.createTempDirectory("codesync-missing-test-").resolve("missing");
        ReflectionTestUtils.invokeMethod(executionService, "deleteDirectory", missing);

        Path directory = Files.createTempDirectory("codesync-delete-test-");
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("nested").resolve("file.txt"), "content");

        ReflectionTestUtils.invokeMethod(executionService, "deleteDirectory", directory);

        assertThat(directory).doesNotExist();
    }

    private ExecutionJob job(String jobId, String status) {
        return ExecutionJob.builder()
                .jobId(jobId)
                .projectId(1L)
                .fileId(2L)
                .userId(3L)
                .language("Python")
                .sourceCode("print('hello')")
                .stdin("")
                .status(status)
                .stdout("")
                .stderr("")
                .build();
    }

    private List<String> shellCommand(String command) {
        return shellCommand(command, command);
    }

    private List<String> shellCommand(String unixCommand, String windowsCommand) {
        return isWindows()
                ? List.of("cmd", "/c", windowsCommand)
                : List.of("/bin/sh", "-c", unixCommand);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @SuppressWarnings("unchecked")
    private void assertPlan(String language, String fileName, List<String> expectedCommand, Path workDir) throws Exception {
        ExecutionJob job = job("job-" + language, "QUEUED");
        job.setLanguage(language);
        job.setSourceCode("source for " + language);

        Object plan = ReflectionTestUtils.invokeMethod(executionService, "createProcessPlan", job, workDir);
        Method commandMethod = plan.getClass().getDeclaredMethod("command");
        commandMethod.setAccessible(true);
        List<String> command = (List<String>) commandMethod.invoke(plan);

        assertThat(command).isEqualTo(expectedCommand);
        assertThat(workDir.resolve(fileName)).hasContent("source for " + language);
    }
}
