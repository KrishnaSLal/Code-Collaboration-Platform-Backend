package com.app.fileservice.controller;

import com.app.fileservice.dto.*;
import com.app.fileservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public FileResponse createFile(@RequestBody FileRequest request) {
        return fileService.createFile(request);
    }

    @PostMapping("/folders")
    public FileResponse createFolder(@RequestBody FolderRequest request) {
        return fileService.createFolder(request);
    }

    @GetMapping("/{fileId}")
    public FileResponse getFileById(@PathVariable Long fileId) {
        return fileService.getFileById(fileId);
    }

    @GetMapping("/project/{projectId}")
    public List<FileResponse> getFilesByProject(@PathVariable Long projectId) {
        return fileService.getFilesByProject(projectId);
    }

    @GetMapping("/{fileId}/content")
    public String getFileContent(@PathVariable Long fileId) {
        return fileService.getFileContent(fileId);
    }

    @PutMapping("/{fileId}/content")
    public FileResponse updateFileContent(@PathVariable Long fileId,
                                          @RequestBody FileContentUpdateRequest request) {
        return fileService.updateFileContent(fileId, request);
    }

    @PutMapping("/{fileId}/rename")
    public FileResponse renameFile(@PathVariable Long fileId,
                                   @RequestBody RenameFileRequest request) {
        return fileService.renameFile(fileId, request);
    }

    @PutMapping("/{fileId}/move")
    public FileResponse moveFile(@PathVariable Long fileId,
                                 @RequestBody MoveFileRequest request) {
        return fileService.moveFile(fileId, request);
    }

    @DeleteMapping("/{fileId}")
    public String deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return "File deleted successfully";
    }

    @PostMapping("/{fileId}/restore")
    public FileResponse restoreFile(@PathVariable Long fileId) {
        return fileService.restoreFile(fileId);
    }

    @GetMapping("/project/{projectId}/tree")
    public List<FileResponse> getFileTree(@PathVariable Long projectId) {
        return fileService.getFileTree(projectId);
    }

    @GetMapping("/project/{projectId}/search")
    public List<FileResponse> searchInProject(@PathVariable Long projectId,
                                              @RequestParam String keyword) {
        return fileService.searchInProject(projectId, keyword);
    }
}