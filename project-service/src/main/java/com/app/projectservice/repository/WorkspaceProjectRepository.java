package com.app.projectservice.repository;

import com.app.projectservice.entity.WorkspaceProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceProjectRepository extends JpaRepository<WorkspaceProject, Long> {

    List<WorkspaceProject> findByOwnerId(Long ownerId);

    List<WorkspaceProject> findByVisibilityAndArchivedFalse(String visibility);

    List<WorkspaceProject> findByLanguageAndArchivedFalse(String language);

    List<WorkspaceProject> findByProjectNameContainingIgnoreCaseAndArchivedFalse(String projectName);
}