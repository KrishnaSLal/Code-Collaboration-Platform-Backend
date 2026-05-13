package com.app.versionservice.service;

import com.app.versionservice.dto.*;

import java.util.List;

public interface VersionService {

    SnapshotResponse createSnapshot(CreateSnapshotRequest request);

    SnapshotResponse getSnapshotById(String snapshotId);

    List<SnapshotResponse> getSnapshotsByFile(Long fileId);

    List<SnapshotResponse> getSnapshotsByProject(Long projectId);

    List<SnapshotResponse> getSnapshotsByBranch(String branch);

    SnapshotResponse getLatestSnapshot(Long fileId);

    SnapshotResponse restoreSnapshot(String snapshotId, RestoreSnapshotRequest request);

    DiffResponse diffSnapshots(String snapshotIdOne, String snapshotIdTwo);

    SnapshotResponse createBranch(CreateBranchRequest request);

    SnapshotResponse tagSnapshot(TagSnapshotRequest request);

    List<SnapshotResponse> getFileHistory(Long fileId);
}