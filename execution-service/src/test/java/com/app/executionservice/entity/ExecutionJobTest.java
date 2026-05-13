package com.app.executionservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionJobTest {

    @Test
    void onCreateInitializesMissingDefaults() {
        ExecutionJob job = ExecutionJob.builder()
                .language("Python")
                .sourceCode("print('hello')")
                .build();

        job.onCreate();

        assertThat(job.getJobId()).isNotBlank();
        assertThat(job.getStatus()).isEqualTo("QUEUED");
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreatePreservesExistingValues() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        ExecutionJob job = ExecutionJob.builder()
                .jobId("job-1")
                .language("Python")
                .status("RUNNING")
                .createdAt(createdAt)
                .build();

        job.onCreate();

        assertThat(job.getJobId()).isEqualTo("job-1");
        assertThat(job.getStatus()).isEqualTo("RUNNING");
        assertThat(job.getCreatedAt()).isEqualTo(createdAt);
    }
}
