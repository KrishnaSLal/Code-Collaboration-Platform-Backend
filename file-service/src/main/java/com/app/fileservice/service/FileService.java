package com.app.fileservice.service;

import com.app.fileservice.dto.*;

import java.util.List;

public interface FileService {

    FileResponse createFile(FileRequest request);

    FileResponse createFolder(FolderRequest request);

    FileResponse getFileById(Long fileId);

    List<FileResponse> getFilesByProject(Long projectId);

    String getFileContent(Long fileId);

    FileResponse updateFileContent(Long fileId, FileContentUpdateRequest request);

    FileResponse renameFile(Long fileId, RenameFileRequest request);

    FileResponse moveFile(Long fileId, MoveFileRequest request);

    void deleteFile(Long fileId);

    FileResponse restoreFile(Long fileId);

    List<FileResponse> getFileTree(Long projectId);

    List<FileResponse> searchInProject(Long projectId, String keyword);
}