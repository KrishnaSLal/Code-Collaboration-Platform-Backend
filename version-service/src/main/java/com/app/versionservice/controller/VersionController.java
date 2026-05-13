package com.app.versionservice.controller;

import com.app.versionservice.dto.*;
import com.app.versionservice.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @PostMapping
    public SnapshotResponse createSnapshot(@RequestBody CreateSnapshotRequest request) {
        return versionService.createSnapshot(request);
    }

    @GetMapping("/{snapshotId}")
    public SnapshotResponse getSnapshotById(@PathVariable String snapshotId) {
        return versionService.getSnapshotById(snapshotId);
    }

    @GetMapping("/file/{fileId}")
    public List<SnapshotResponse> getSnapshotsByFile(@PathVariable Long fileId) {
        return versionService.getSnapshotsByFile(fileId);
    }

    @GetMapping("/project/{projectId}")
    public List<SnapshotResponse> getSnapshotsByProject(@PathVariable Long projectId) {
        return versionService.getSnapshotsByProject(projectId);
    }

    @GetMapping("/branch/{branch}")
    public List<SnapshotResponse> getSnapshotsByBranch(@PathVariable String branch) {
        return versionService.getSnapshotsByBranch(branch);
    }

    @GetMapping("/latest/{fileId}")
    public SnapshotResponse getLatestSnapshot(@PathVariable Long fileId) {
        return versionService.getLatestSnapshot(fileId);
    }

    @PostMapping("/{snapshotId}/restore")
    public SnapshotResponse restoreSnapshot(@PathVariable String snapshotId,
                                            @RequestBody RestoreSnapshotRequest request) {
        return versionService.restoreSnapshot(snapshotId, request);
    }

    @GetMapping("/diff")
    public DiffResponse diffSnapshots(@RequestParam String snapshotIdOne,
                                      @RequestParam String snapshotIdTwo) {
        return versionService.diffSnapshots(snapshotIdOne, snapshotIdTwo);
    }

    @PostMapping("/branch")
    public SnapshotResponse createBranch(@RequestBody CreateBranchRequest request) {
        return versionService.createBranch(request);
    }

    @PostMapping("/tag")
    public SnapshotResponse tagSnapshot(@RequestBody TagSnapshotRequest request) {
        return versionService.tagSnapshot(request);
    }

    @GetMapping("/history/{fileId}")
    public List<SnapshotResponse> getFileHistory(@PathVariable Long fileId) {
        return versionService.getFileHistory(fileId);
    }
}