package com.app.versionservice.service;

import com.app.versionservice.dto.*;
import com.app.versionservice.entity.Snapshot;
import com.app.versionservice.exception.SnapshotNotFoundException;
import com.app.versionservice.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VersionServiceImpl implements VersionService {

    private final SnapshotRepository repository;

    @Override
    public SnapshotResponse createSnapshot(CreateSnapshotRequest request) {
        Snapshot snapshot = Snapshot.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .authorId(request.getAuthorId())
                .message(request.getMessage())
                .content(request.getContent())
                .hash(generateHash(request.getContent()))
                .parentSnapshotId(request.getParentSnapshotId())
                .branch(request.getBranch())
                .build();

        return mapToResponse(repository.save(snapshot));
    }

    @Override
    public SnapshotResponse getSnapshotById(String snapshotId) {
        return mapToResponse(fetchSnapshot(snapshotId));
    }

    @Override
    public List<SnapshotResponse> getSnapshotsByFile(Long fileId) {
        return repository.findByFileId(fileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SnapshotResponse> getSnapshotsByProject(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SnapshotResponse> getSnapshotsByBranch(String branch) {
        return repository.findByBranch(branch)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SnapshotResponse getLatestSnapshot(Long fileId) {
        Snapshot snapshot = repository.findTopByFileIdOrderByCreatedAtDesc(fileId)
                .orElseThrow(() -> new SnapshotNotFoundException("No snapshots found for file: " + fileId));
        return mapToResponse(snapshot);
    }

    @Override
    public SnapshotResponse restoreSnapshot(String snapshotId, RestoreSnapshotRequest request) {
        Snapshot existingSnapshot = fetchSnapshot(snapshotId);

        Snapshot restoredSnapshot = Snapshot.builder()
                .projectId(existingSnapshot.getProjectId())
                .fileId(existingSnapshot.getFileId())
                .authorId(request.getAuthorId())
                .message(request.getMessage())
                .content(existingSnapshot.getContent())
                .hash(generateHash(existingSnapshot.getContent()))
                .parentSnapshotId(existingSnapshot.getSnapshotId())
                .branch(existingSnapshot.getBranch())
                .build();

        return mapToResponse(repository.save(restoredSnapshot));
    }

    @Override
    public DiffResponse diffSnapshots(String snapshotIdOne, String snapshotIdTwo) {
        Snapshot first = fetchSnapshot(snapshotIdOne);
        Snapshot second = fetchSnapshot(snapshotIdTwo);

        String diff = buildSimpleDiff(first.getContent(), second.getContent());

        return DiffResponse.builder()
                .snapshotIdOne(snapshotIdOne)
                .snapshotIdTwo(snapshotIdTwo)
                .diffResult(diff)
                .build();
    }

    @Override
    public SnapshotResponse createBranch(CreateBranchRequest request) {
        Snapshot snapshot = fetchSnapshot(request.getSnapshotId());
        snapshot.setBranch(request.getBranchName());
        return mapToResponse(repository.save(snapshot));
    }

    @Override
    public SnapshotResponse tagSnapshot(TagSnapshotRequest request) {
        Snapshot snapshot = fetchSnapshot(request.getSnapshotId());
        snapshot.setTag(request.getTag());
        return mapToResponse(repository.save(snapshot));
    }

    @Override
    public List<SnapshotResponse> getFileHistory(Long fileId) {
        return repository.findByFileId(fileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Snapshot fetchSnapshot(String snapshotId) {
        return repository.findBySnapshotId(snapshotId)
                .orElseThrow(() -> new SnapshotNotFoundException("Snapshot not found with id: " + snapshotId));
    }

    private SnapshotResponse mapToResponse(Snapshot snapshot) {
        return SnapshotResponse.builder()
                .snapshotId(snapshot.getSnapshotId())
                .projectId(snapshot.getProjectId())
                .fileId(snapshot.getFileId())
                .authorId(snapshot.getAuthorId())
                .message(snapshot.getMessage())
                .content(snapshot.getContent())
                .hash(snapshot.getHash())
                .parentSnapshotId(snapshot.getParentSnapshotId())
                .branch(snapshot.getBranch())
                .tag(snapshot.getTag())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }

    private String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate SHA-256 hash");
        }
    }

    private String buildSimpleDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent == null ? new String[0] : oldContent.split("\n");
        String[] newLines = newContent == null ? new String[0] : newContent.split("\n");

        StringBuilder diff = new StringBuilder();
        int max = Math.max(oldLines.length, newLines.length);

        for (int i = 0; i < max; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;

            if (oldLine == null) {
                diff.append("+ ").append(newLine).append("\n");
            } else if (newLine == null) {
                diff.append("- ").append(oldLine).append("\n");
            } else if (!oldLine.equals(newLine)) {
                diff.append("- ").append(oldLine).append("\n");
                diff.append("+ ").append(newLine).append("\n");
            } else {
                diff.append("  ").append(oldLine).append("\n");
            }
        }

        return diff.toString();
    }
}