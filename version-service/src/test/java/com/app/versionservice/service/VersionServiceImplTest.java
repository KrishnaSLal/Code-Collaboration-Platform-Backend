package com.app.versionservice.service;

import com.app.versionservice.dto.CreateBranchRequest;
import com.app.versionservice.dto.CreateSnapshotRequest;
import com.app.versionservice.dto.DiffResponse;
import com.app.versionservice.dto.RestoreSnapshotRequest;
import com.app.versionservice.dto.SnapshotResponse;
import com.app.versionservice.dto.TagSnapshotRequest;
import com.app.versionservice.entity.Snapshot;
import com.app.versionservice.exception.SnapshotNotFoundException;
import com.app.versionservice.repository.SnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceImplTest {

    @Mock
    private SnapshotRepository repository;

    @InjectMocks
    private VersionServiceImpl versionService;

    @Test
    void createSnapshotGeneratesSha256HashAndMapsResponse() {
        CreateSnapshotRequest request = CreateSnapshotRequest.builder()
                .projectId(1L)
                .fileId(2L)
                .authorId(3L)
                .message("Initial commit")
                .content("class Main {}")
                .branch("main")
                .build();

        when(repository.save(any(Snapshot.class))).thenAnswer(invocation -> {
            Snapshot snapshot = invocation.getArgument(0);
            snapshot.setSnapshotId("snapshot-1");
            return snapshot;
        });

        SnapshotResponse response = versionService.createSnapshot(request);

        assertThat(response.getSnapshotId()).isEqualTo("snapshot-1");
        assertThat(response.getHash()).isEqualTo(sha256("class Main {}"));
        assertThat(response.getBranch()).isEqualTo("main");
    }

    @Test
    void restoreSnapshotCreatesNewSnapshotFromExistingContent() {
        Snapshot existing = snapshot("snapshot-1", "old content");
        when(repository.findBySnapshotId("snapshot-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(Snapshot.class))).thenAnswer(invocation -> {
            Snapshot restored = invocation.getArgument(0);
            restored.setSnapshotId("snapshot-2");
            return restored;
        });

        SnapshotResponse response = versionService.restoreSnapshot("snapshot-1",
                RestoreSnapshotRequest.builder()
                        .authorId(9L)
                        .message("Restore old content")
                        .build());

        assertThat(response.getSnapshotId()).isEqualTo("snapshot-2");
        assertThat(response.getContent()).isEqualTo("old content");
        assertThat(response.getParentSnapshotId()).isEqualTo("snapshot-1");
        assertThat(response.getAuthorId()).isEqualTo(9L);
    }

    @Test
    void getSnapshotByIdReturnsMappedSnapshot() {
        when(repository.findBySnapshotId("snapshot-1"))
                .thenReturn(Optional.of(snapshot("snapshot-1", "content")));

        SnapshotResponse response = versionService.getSnapshotById("snapshot-1");

        assertThat(response.getSnapshotId()).isEqualTo("snapshot-1");
        assertThat(response.getContent()).isEqualTo("content");
    }

    @Test
    void diffSnapshotsReportsChangedLines() {
        when(repository.findBySnapshotId("one")).thenReturn(Optional.of(snapshot("one", "line 1\nold line")));
        when(repository.findBySnapshotId("two")).thenReturn(Optional.of(snapshot("two", "line 1\nnew line")));

        DiffResponse response = versionService.diffSnapshots("one", "two");

        assertThat(response.getSnapshotIdOne()).isEqualTo("one");
        assertThat(response.getSnapshotIdTwo()).isEqualTo("two");
        assertThat(response.getDiffResult()).contains("  line 1");
        assertThat(response.getDiffResult()).contains("- old line");
        assertThat(response.getDiffResult()).contains("+ new line");
    }

    @Test
    void createBranchUpdatesExistingSnapshotBranch() {
        Snapshot snapshot = snapshot("snapshot-1", "content");
        when(repository.findBySnapshotId("snapshot-1")).thenReturn(Optional.of(snapshot));
        when(repository.save(any(Snapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotResponse response = versionService.createBranch(CreateBranchRequest.builder()
                .snapshotId("snapshot-1")
                .branchName("feature/login")
                .build());

        assertThat(response.getBranch()).isEqualTo("feature/login");
    }

    @Test
    void tagSnapshotUpdatesExistingSnapshotTag() {
        Snapshot snapshot = snapshot("snapshot-1", "content");
        when(repository.findBySnapshotId("snapshot-1")).thenReturn(Optional.of(snapshot));
        when(repository.save(any(Snapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SnapshotResponse response = versionService.tagSnapshot(TagSnapshotRequest.builder()
                .snapshotId("snapshot-1")
                .tag("v1.0")
                .build());

        assertThat(response.getTag()).isEqualTo("v1.0");
    }

    @Test
    void getSnapshotsByFileMapsRepositoryResults() {
        when(repository.findByFileId(2L)).thenReturn(List.of(snapshot("snapshot-1", "content")));

        List<SnapshotResponse> responses = versionService.getSnapshotsByFile(2L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSnapshotId()).isEqualTo("snapshot-1");
    }

    @Test
    void getSnapshotsByProjectMapsRepositoryResults() {
        when(repository.findByProjectId(1L)).thenReturn(List.of(snapshot("snapshot-1", "content")));

        List<SnapshotResponse> responses = versionService.getSnapshotsByProject(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void getSnapshotsByBranchMapsRepositoryResults() {
        when(repository.findByBranch("main")).thenReturn(List.of(snapshot("snapshot-1", "content")));

        List<SnapshotResponse> responses = versionService.getSnapshotsByBranch("main");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBranch()).isEqualTo("main");
    }

    @Test
    void getLatestSnapshotReturnsLatestRepositoryResult() {
        when(repository.findTopByFileIdOrderByCreatedAtDesc(2L))
                .thenReturn(Optional.of(snapshot("latest", "content")));

        SnapshotResponse response = versionService.getLatestSnapshot(2L);

        assertThat(response.getSnapshotId()).isEqualTo("latest");
    }

    @Test
    void getFileHistoryMapsRepositoryResults() {
        when(repository.findByFileId(2L))
                .thenReturn(List.of(snapshot("snapshot-1", "content"), snapshot("snapshot-2", "new content")));

        List<SnapshotResponse> responses = versionService.getFileHistory(2L);

        assertThat(responses)
                .extracting(SnapshotResponse::getSnapshotId)
                .containsExactly("snapshot-1", "snapshot-2");
    }

    @Test
    void getLatestSnapshotThrowsWhenMissing() {
        when(repository.findTopByFileIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionService.getLatestSnapshot(2L))
                .isInstanceOf(SnapshotNotFoundException.class)
                .hasMessage("No snapshots found for file: 2");
    }

    private Snapshot snapshot(String id, String content) {
        return Snapshot.builder()
                .snapshotId(id)
                .projectId(1L)
                .fileId(2L)
                .authorId(3L)
                .message("message")
                .content(content)
                .hash(sha256(content))
                .branch("main")
                .build();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hex.append(String.format("%02x", hashByte));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
