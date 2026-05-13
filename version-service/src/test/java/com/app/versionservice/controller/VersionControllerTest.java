package com.app.versionservice.controller;

import com.app.versionservice.dto.CreateBranchRequest;
import com.app.versionservice.dto.CreateSnapshotRequest;
import com.app.versionservice.dto.DiffResponse;
import com.app.versionservice.dto.RestoreSnapshotRequest;
import com.app.versionservice.dto.SnapshotResponse;
import com.app.versionservice.dto.TagSnapshotRequest;
import com.app.versionservice.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock
    private VersionService versionService;

    private VersionController controller;

    @BeforeEach
    void setUp() {
        controller = new VersionController(versionService);
    }

    @Test
    void delegatesVersionOperations() {
        CreateSnapshotRequest createRequest = CreateSnapshotRequest.builder().content("v1").build();
        RestoreSnapshotRequest restoreRequest = RestoreSnapshotRequest.builder().authorId(7L).build();
        CreateBranchRequest branchRequest = CreateBranchRequest.builder().branchName("feature").build();
        TagSnapshotRequest tagRequest = TagSnapshotRequest.builder().tag("v1.0").build();
        SnapshotResponse response = SnapshotResponse.builder().snapshotId("snap-1").build();
        DiffResponse diff = DiffResponse.builder().snapshotIdOne("snap-1").snapshotIdTwo("snap-2").build();

        when(versionService.createSnapshot(createRequest)).thenReturn(response);
        when(versionService.getSnapshotById("snap-1")).thenReturn(response);
        when(versionService.getSnapshotsByFile(2L)).thenReturn(List.of(response));
        when(versionService.getSnapshotsByProject(5L)).thenReturn(List.of(response));
        when(versionService.getSnapshotsByBranch("main")).thenReturn(List.of(response));
        when(versionService.getLatestSnapshot(2L)).thenReturn(response);
        when(versionService.restoreSnapshot("snap-1", restoreRequest)).thenReturn(response);
        when(versionService.diffSnapshots("snap-1", "snap-2")).thenReturn(diff);
        when(versionService.createBranch(branchRequest)).thenReturn(response);
        when(versionService.tagSnapshot(tagRequest)).thenReturn(response);
        when(versionService.getFileHistory(2L)).thenReturn(List.of(response));

        assertThat(controller.createSnapshot(createRequest)).isSameAs(response);
        assertThat(controller.getSnapshotById("snap-1")).isSameAs(response);
        assertThat(controller.getSnapshotsByFile(2L)).containsExactly(response);
        assertThat(controller.getSnapshotsByProject(5L)).containsExactly(response);
        assertThat(controller.getSnapshotsByBranch("main")).containsExactly(response);
        assertThat(controller.getLatestSnapshot(2L)).isSameAs(response);
        assertThat(controller.restoreSnapshot("snap-1", restoreRequest)).isSameAs(response);
        assertThat(controller.diffSnapshots("snap-1", "snap-2")).isSameAs(diff);
        assertThat(controller.createBranch(branchRequest)).isSameAs(response);
        assertThat(controller.tagSnapshot(tagRequest)).isSameAs(response);
        assertThat(controller.getFileHistory(2L)).containsExactly(response);
    }
}
