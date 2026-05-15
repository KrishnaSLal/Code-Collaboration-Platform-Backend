package com.app.executionservice.service;

import com.app.executionservice.dto.ExecutionRequest;
import com.app.executionservice.dto.ExecutionResponse;
import com.app.executionservice.dto.ExecutionStatsResponse;
import com.app.executionservice.dto.LanguageInfoResponse;
import com.app.executionservice.entity.ExecutionJob;
import com.app.executionservice.exception.ExecutionJobNotFoundException;
import com.app.executionservice.repository.ExecutionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private static final long EXECUTION_TIMEOUT_SECONDS = 10;

    private final ExecutionJobRepository repository;

    @Override
    public ExecutionResponse submitExecution(ExecutionRequest request) {
        ExecutionJob job = ExecutionJob.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .userId(request.getUserId())
                .language(request.getLanguage())
                .sourceCode(request.getSourceCode())
                .stdin(request.getStdin())
                .status("QUEUED")
                .stdout("")
                .stderr("")
                .build();

        ExecutionJob savedJob = repository.save(job);
        return mapToResponse(runJob(savedJob));
    }

    @Override
    public ExecutionResponse getJobById(String jobId) {
        return mapToResponse(fetchJob(jobId));
    }

    @Override
    public List<ExecutionResponse> getExecutionsByUser(Long userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionResponse> getExecutionsByProject(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExecutionResponse cancelExecution(String jobId) {
        ExecutionJob job = fetchJob(jobId);

        if ("COMPLETED".equalsIgnoreCase(job.getStatus()) ||
                "FAILED".equalsIgnoreCase(job.getStatus()) ||
                "TIMED_OUT".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Cannot cancel a finished execution job");
        }

        job.setStatus("CANCELLED");
        job.setCompletedAt(LocalDateTime.now());

        return mapToResponse(repository.save(job));
    }

    @Override
    public ExecutionResponse getExecutionResult(String jobId) {
        return mapToResponse(fetchJob(jobId));
    }

    @Override
    public List<LanguageInfoResponse> getSupportedLanguages() {
        return Arrays.asList(
                new LanguageInfoResponse("Python", "3.x"),
                new LanguageInfoResponse("Java", "17"),
                new LanguageInfoResponse("JavaScript", "Node.js"),
                new LanguageInfoResponse("C", "GCC"),
                new LanguageInfoResponse("C++", "G++"),
                new LanguageInfoResponse("Go", "1.x"),
                new LanguageInfoResponse("Rust", "stable"),
                new LanguageInfoResponse("Ruby", "3.x"),
                new LanguageInfoResponse("TypeScript", "5.x"),
                new LanguageInfoResponse("PHP", "8.x"),
                new LanguageInfoResponse("Kotlin", "1.9"),
                new LanguageInfoResponse("Swift", "5.x"),
                new LanguageInfoResponse("R", "4.x")
        );
    }

    @Override
    public LanguageInfoResponse getLanguageVersion(String language) {
        return getSupportedLanguages().stream()
                .filter(lang -> lang.getLanguage().equalsIgnoreCase(language))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Language not supported: " + language));
    }

    @Override
    public ExecutionStatsResponse getExecutionStats() {
        long total = repository.count();
        long completed = repository.countByStatus("COMPLETED");
        long failed = repository.countByStatus("FAILED");
        long cancelled = repository.countByStatus("CANCELLED");

        return ExecutionStatsResponse.builder()
                .totalExecutions(total)
                .completedExecutions(completed)
                .failedExecutions(failed)
                .cancelledExecutions(cancelled)
                .build();
    }

    @Override
    public ExecutionStatsResponse getExecutionStatsByUser(Long userId) {
        long total = repository.countByUserId(userId);
        long completed = repository.countByUserIdAndStatus(userId, "COMPLETED");
        long failed = repository.countByUserIdAndStatus(userId, "FAILED");
        long cancelled = repository.countByUserIdAndStatus(userId, "CANCELLED");

        return ExecutionStatsResponse.builder()
                .totalExecutions(total)
                .completedExecutions(completed)
                .failedExecutions(failed)
                .cancelledExecutions(cancelled)
                .build();
    }

    @Override
    @Transactional
    public void deleteExecutionsByProject(Long projectId) {
        repository.deleteByProjectId(projectId);
    }

    private ExecutionJob fetchJob(String jobId) {
        return repository.findByJobId(jobId)
                .orElseThrow(() -> new ExecutionJobNotFoundException("Execution job not found with id: " + jobId));
    }

    private ExecutionJob runJob(ExecutionJob job) {
        job.setStatus("RUNNING");
        repository.save(job);

        long startedAt = System.currentTimeMillis();

        try {
            RunResult result = executeSource(job);
            job.setStdout(result.stdout());
            job.setStderr(result.stderr());
            job.setExitCode(result.exitCode());
            job.setStatus(result.timedOut() ? "TIMED_OUT" : result.exitCode() == 0 ? "COMPLETED" : "FAILED");
        } catch (Exception ex) {
            job.setStdout("");
            job.setStderr(ex.getMessage());
            job.setExitCode(-1);
            job.setStatus("FAILED");
        }

        job.setExecutionTimeMs(System.currentTimeMillis() - startedAt);
        job.setCompletedAt(LocalDateTime.now());
        return repository.save(job);
    }

    private RunResult executeSource(ExecutionJob job) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("codesync-exec-");

        try {
            ProcessPlan plan = createProcessPlan(job, workDir);
            ProcessBuilder builder = new ProcessBuilder(plan.command());
            builder.directory(workDir.toFile());
            configurePythonEnvironment(builder, plan.command().get(0));

            Process process = builder.start();
            if (job.getStdin() != null && !job.getStdin().isBlank()) {
                process.getOutputStream().write(job.getStdin().getBytes(StandardCharsets.UTF_8));
            }
            process.getOutputStream().close();

            boolean finished = process.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new RunResult(
                        readProcessOutput(process.getInputStream().readAllBytes()),
                        "Execution timed out after " + EXECUTION_TIMEOUT_SECONDS + " seconds.",
                        -1,
                        true
                );
            }

            return new RunResult(
                    readProcessOutput(process.getInputStream().readAllBytes()),
                    readProcessOutput(process.getErrorStream().readAllBytes()),
                    process.exitValue(),
                    false
            );
        } finally {
            deleteDirectory(workDir);
        }
    }

    private ProcessPlan createProcessPlan(ExecutionJob job, Path workDir) throws IOException {
        String language = job.getLanguage() == null ? "" : job.getLanguage().trim().toLowerCase();
        String sourceCode = job.getSourceCode() == null ? "" : job.getSourceCode();

        return switch (language) {
            case "javascript", "node.js", "node" -> {
                Path file = workDir.resolve("Main.js");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(List.of("node", file.getFileName().toString()));
            }
            case "typescript" -> {
                Path file = workDir.resolve("Main.ts");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(List.of("npx", "tsx", file.getFileName().toString()));
            }
            case "python", "python3" -> {
                Path file = workDir.resolve("main.py");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(List.of(resolvePythonExecutable(), file.getFileName().toString()));
            }
            case "java" -> {
                Path file = workDir.resolve("Main.java");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(shellCommand("javac Main.java && java Main"));
            }
            case "c" -> {
                Path file = workDir.resolve("main.c");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(shellCommand("gcc main.c -o main && ./main", "gcc main.c -o main.exe && main.exe"));
            }
            case "c++", "cpp", "cplusplus" -> {
                Path file = workDir.resolve("main.cpp");
                Files.writeString(file, sourceCode, StandardCharsets.UTF_8);
                yield new ProcessPlan(shellCommand("g++ main.cpp -o main && ./main", "g++ main.cpp -o main.exe && main.exe"));
            }
            default -> throw new IllegalArgumentException("Language is not runnable by this service: " + job.getLanguage());
        };
    }

    private List<String> shellCommand(String command) {
        return shellCommand(command, command);
    }

    private List<String> shellCommand(String unixCommand, String windowsCommand) {
        if (isWindows()) {
            return List.of("cmd", "/c", windowsCommand);
        }

        return List.of("/bin/sh", "-c", unixCommand);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String readProcessOutput(byte[] output) {
        return new String(output, StandardCharsets.UTF_8);
    }

    private String resolvePythonExecutable() {
        String configuredPath = System.getenv("PYTHON_EXECUTABLE");
        if (isRunnableExecutable(configuredPath)) {
            return configuredPath;
        }

        List<String> candidates = List.of(
                "python",
                "python3",
                "py",
                "C:\\Program Files\\Python312\\python.exe",
                "C:\\Program Files\\Python311\\python.exe",
                "C:\\Program Files\\Python310\\python.exe",
                "C:\\Users\\krish\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                "C:\\Users\\krish\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                "C:\\Users\\krish\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
                "C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\python.exe"
        );

        return candidates.stream()
                .filter(this::isRunnableExecutable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Python is not installed or not on PATH. Install Python 3, add it to PATH, or set PYTHON_EXECUTABLE."
                ));
    }

    private boolean isRunnableExecutable(String executable) {
        if (executable == null || executable.isBlank()) {
            return false;
        }

        try {
            Process process = new ProcessBuilder(executable, "--version").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void configurePythonEnvironment(ProcessBuilder builder, String executable) {
        if (executable == null || !executable.toLowerCase().endsWith("python.exe")) {
            return;
        }

        Path executablePath = Path.of(executable);
        Path embeddedPythonHome = executablePath.getParent().resolve("python");
        if (Files.isDirectory(embeddedPythonHome.resolve("lib").resolve("encodings"))) {
            builder.environment().put("PYTHONHOME", embeddedPythonHome.toString());
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            List<Path> sortedPaths = paths
                    .sorted((left, right) -> right.compareTo(left))
                    .toList();

            for (Path path : sortedPaths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record ProcessPlan(List<String> command) {
    }

    private record RunResult(String stdout, String stderr, int exitCode, boolean timedOut) {
    }

    private ExecutionResponse mapToResponse(ExecutionJob job) {
        return ExecutionResponse.builder()
                .jobId(job.getJobId())
                .projectId(job.getProjectId())
                .fileId(job.getFileId())
                .userId(job.getUserId())
                .language(job.getLanguage())
                .sourceCode(job.getSourceCode())
                .stdin(job.getStdin())
                .status(job.getStatus())
                .stdout(job.getStdout())
                .stderr(job.getStderr())
                .exitCode(job.getExitCode())
                .executionTimeMs(job.getExecutionTimeMs())
                .memoryUsedKb(job.getMemoryUsedKb())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
