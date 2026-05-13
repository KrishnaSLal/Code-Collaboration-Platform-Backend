package com.app.versionservice.repository;

import com.app.versionservice.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<Snapshot, String> {

    List<Snapshot> findByProjectId(Long projectId);

    List<Snapshot> findByFileId(Long fileId);

    List<Snapshot> findByAuthorId(Long authorId);

    List<Snapshot> findByBranch(String branch);

    Optional<Snapshot> findBySnapshotId(String snapshotId);

    Optional<Snapshot> findByHash(String hash);

    Optional<Snapshot> findByTag(String tag);

    Optional<Snapshot> findTopByFileIdOrderByCreatedAtDesc(Long fileId);
}