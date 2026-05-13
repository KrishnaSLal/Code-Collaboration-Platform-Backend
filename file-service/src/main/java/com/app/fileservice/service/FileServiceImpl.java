package com.app.fileservice.service;

import com.app.fileservice.dto.*;
import com.app.fileservice.entity.ProjectFile;
import com.app.fileservice.exception.FileNotFoundException;
import com.app.fileservice.repository.ProjectFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final ProjectFileRepository repository;

    @Override
    public FileResponse createFile(FileRequest request) {
        ProjectFile file = ProjectFile.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .language(request.getLanguage())
                .content(request.getContent() == null ? "" : request.getContent())
                .size((long) (request.getContent() == null ? 0 : request.getContent().length()))
                .folder(false)
                .createdById(request.getCreatedById())
                .lastEditedBy(request.getCreatedById())
                .deleted(false)
                .build();

        return mapToResponse(repository.save(file));
    }

    @Override
    public FileResponse createFolder(FolderRequest request) {
        ProjectFile folder = ProjectFile.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .language(null)
                .content("")
                .size(0L)
                .folder(true)
                .createdById(request.getCreatedById())
                .lastEditedBy(request.getCreatedById())
                .deleted(false)
                .build();

        return mapToResponse(repository.save(folder));
    }

    @Override
    public FileResponse getFileById(Long fileId) {
        return mapToResponse(fetchActiveFile(fileId));
    }

    @Override
    public List<FileResponse> getFilesByProject(Long projectId) {
        return repository.findByProjectIdAndDeletedFalse(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String getFileContent(Long fileId) {
        return fetchActiveFile(fileId).getContent();
    }

    @Override
    public FileResponse updateFileContent(Long fileId, FileContentUpdateRequest request) {
        ProjectFile file = fetchActiveFile(fileId);

        file.setContent(request.getContent());
        file.setSize((long) (request.getContent() == null ? 0 : request.getContent().length()));
        file.setLastEditedBy(request.getLastEditedBy());

        return mapToResponse(repository.save(file));
    }

    @Override
    public FileResponse renameFile(Long fileId, RenameFileRequest request) {
        ProjectFile file = fetchActiveFile(fileId);

        String oldPath = file.getPath();
        String parentPath = "";
        int lastSlashIndex = oldPath.lastIndexOf("/");

        if (lastSlashIndex != -1) {
            parentPath = oldPath.substring(0, lastSlashIndex);
            file.setPath(parentPath + "/" + request.getNewName());
        } else {
            file.setPath(request.getNewName());
        }

        file.setName(request.getNewName());

        return mapToResponse(repository.save(file));
    }

    @Override
    public FileResponse moveFile(Long fileId, MoveFileRequest request) {
        ProjectFile file = fetchActiveFile(fileId);
        file.setPath(request.getNewPath());
        return mapToResponse(repository.save(file));
    }

    @Override
    public void deleteFile(Long fileId) {
        ProjectFile file = fetchActiveFile(fileId);
        file.setDeleted(true);
        repository.save(file);
    }

    @Override
    public FileResponse restoreFile(Long fileId) {
        ProjectFile file = repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + fileId));

        file.setDeleted(false);
        return mapToResponse(repository.save(file));
    }

    @Override
    public List<FileResponse> getFileTree(Long projectId) {
        return repository.findByProjectIdAndDeletedFalse(projectId)
                .stream()
                .sorted((a, b) -> a.getPath().compareToIgnoreCase(b.getPath()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileResponse> searchInProject(Long projectId, String keyword) {
        return repository.findByProjectIdAndDeletedFalse(projectId)
                .stream()
                .filter(file ->
                        (file.getName() != null && file.getName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (file.getPath() != null && file.getPath().toLowerCase().contains(keyword.toLowerCase())) ||
                        (file.getContent() != null && file.getContent().toLowerCase().contains(keyword.toLowerCase()))
                )
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProjectFile fetchActiveFile(Long fileId) {
        return repository.findByFileIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new FileNotFoundException("Active file not found with id: " + fileId));
    }

    private FileResponse mapToResponse(ProjectFile file) {
        return FileResponse.builder()
                .fileId(file.getFileId())
                .projectId(file.getProjectId())
                .name(file.getName())
                .path(file.getPath())
                .language(file.getLanguage())
                .content(file.getContent())
                .size(file.getSize())
                .folder(file.getFolder())
                .createdById(file.getCreatedById())
                .lastEditedBy(file.getLastEditedBy())
                .deleted(file.getDeleted())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}