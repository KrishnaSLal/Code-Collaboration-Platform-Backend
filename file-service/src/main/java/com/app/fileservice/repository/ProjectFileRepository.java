package com.app.fileservice.repository;

import com.app.fileservice.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findByProjectIdAndDeletedFalse(Long projectId);

    Optional<ProjectFile> findByFileIdAndDeletedFalse(Long fileId);

    Optional<ProjectFile> findByProjectIdAndPathAndDeletedFalse(Long projectId, String path);

    List<ProjectFile> findByProjectIdAndLanguageAndDeletedFalse(Long projectId, String language);

    List<ProjectFile> findByProjectIdAndFolderFalseAndDeletedFalse(Long projectId);

    List<ProjectFile> findByProjectIdAndDeletedTrue(Long projectId);

    List<ProjectFile> findByProjectIdAndPathContainingIgnoreCaseAndDeletedFalse(Long projectId, String path);

    List<ProjectFile> findByProjectIdAndNameContainingIgnoreCaseAndDeletedFalse(Long projectId, String name);
}