package com.app.fileservice.service;

import com.app.fileservice.dto.FileContentUpdateRequest;
import com.app.fileservice.dto.FileRequest;
import com.app.fileservice.dto.FileResponse;
import com.app.fileservice.dto.FolderRequest;
import com.app.fileservice.dto.MoveFileRequest;
import com.app.fileservice.dto.RenameFileRequest;
import com.app.fileservice.entity.ProjectFile;
import com.app.fileservice.exception.FileNotFoundException;
import com.app.fileservice.repository.ProjectFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private ProjectFileRepository repository;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    void createFileDefaultsNullContentToEmptyStringAndZeroSize() {
        FileRequest request = FileRequest.builder()
                .projectId(1L)
                .name("Main.java")
                .path("src/Main.java")
                .language("Java")
                .content(null)
                .createdById(7L)
                .build();

        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            file.setFileId(10L);
            return file;
        });

        FileResponse response = fileService.createFile(request);

        assertThat(response.getFileId()).isEqualTo(10L);
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getSize()).isZero();
        assertThat(response.getFolder()).isFalse();
        assertThat(response.getDeleted()).isFalse();
        assertThat(response.getLastEditedBy()).isEqualTo(7L);
    }

    @Test
    void updateFileContentRecalculatesSizeAndEditor() {
        ProjectFile file = activeFile(10L, "src/Main.java", "old");
        when(repository.findByFileIdAndDeletedFalse(10L)).thenReturn(Optional.of(file));
        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileContentUpdateRequest request = FileContentUpdateRequest.builder()
                .content("new content")
                .lastEditedBy(9L)
                .build();

        FileResponse response = fileService.updateFileContent(10L, request);

        assertThat(response.getContent()).isEqualTo("new content");
        assertThat(response.getSize()).isEqualTo(11L);
        assertThat(response.getLastEditedBy()).isEqualTo(9L);
    }

    @Test
    void createFolderCreatesFolderWithEmptyContentAndZeroSize() {
        FolderRequest request = FolderRequest.builder()
                .projectId(5L)
                .name("src")
                .path("src")
                .createdById(7L)
                .build();

        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile folder = invocation.getArgument(0);
            folder.setFileId(20L);
            return folder;
        });

        FileResponse response = fileService.createFolder(request);

        assertThat(response.getFileId()).isEqualTo(20L);
        assertThat(response.getFolder()).isTrue();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getSize()).isZero();
        assertThat(response.getDeleted()).isFalse();
    }

    @Test
    void getFilesByProjectReturnsActiveProjectFiles() {
        when(repository.findByProjectIdAndDeletedFalse(5L))
                .thenReturn(List.of(activeFile(1L, "src/Main.java", "class Main {}")));

        List<FileResponse> responses = fileService.getFilesByProject(5L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectId()).isEqualTo(5L);
    }

    @Test
    void getFileContentReturnsActiveFileContent() {
        when(repository.findByFileIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(activeFile(10L, "src/Main.java", "class Main {}")));

        String content = fileService.getFileContent(10L);

        assertThat(content).isEqualTo("class Main {}");
    }

    @Test
    void renameFilePreservesParentPath() {
        ProjectFile file = activeFile(10L, "src/Main.java", "class Main {}");
        when(repository.findByFileIdAndDeletedFalse(10L)).thenReturn(Optional.of(file));
        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.renameFile(10L, RenameFileRequest.builder()
                .newName("App.java")
                .build());

        assertThat(response.getName()).isEqualTo("App.java");
        assertThat(response.getPath()).isEqualTo("src/App.java");
    }

    @Test
    void moveFileUpdatesPath() {
        ProjectFile file = activeFile(10L, "src/Main.java", "class Main {}");
        when(repository.findByFileIdAndDeletedFalse(10L)).thenReturn(Optional.of(file));
        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.moveFile(10L, MoveFileRequest.builder()
                .newPath("app/Main.java")
                .build());

        assertThat(response.getPath()).isEqualTo("app/Main.java");
    }

    @Test
    void searchInProjectMatchesNamePathOrContentIgnoringCase() {
        ProjectFile matchingByContent = activeFile(1L, "src/Main.java", "contains RabbitMQ integration");
        matchingByContent.setName("Main.java");
        ProjectFile nonMatching = activeFile(2L, "README.md", "documentation");
        nonMatching.setName("README.md");

        when(repository.findByProjectIdAndDeletedFalse(5L))
                .thenReturn(List.of(matchingByContent, nonMatching));

        List<FileResponse> responses = fileService.searchInProject(5L, "rabbitmq");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFileId()).isEqualTo(1L);
    }

    @Test
    void deleteFileMarksActiveFileAsDeleted() {
        ProjectFile file = activeFile(10L, "src/Main.java", "class Main {}");
        when(repository.findByFileIdAndDeletedFalse(10L)).thenReturn(Optional.of(file));

        fileService.deleteFile(10L);

        assertThat(file.getDeleted()).isTrue();
        verify(repository).save(file);
    }

    @Test
    void restoreFileMarksDeletedFileAsActive() {
        ProjectFile file = activeFile(10L, "src/Main.java", "class Main {}");
        file.setDeleted(true);
        when(repository.findById(10L)).thenReturn(Optional.of(file));
        when(repository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = fileService.restoreFile(10L);

        assertThat(response.getDeleted()).isFalse();
    }

    @Test
    void getFileTreeSortsFilesByPathIgnoringCase() {
        ProjectFile zFile = activeFile(1L, "zeta/Main.java", "z");
        ProjectFile aFile = activeFile(2L, "alpha/Main.java", "a");
        when(repository.findByProjectIdAndDeletedFalse(5L)).thenReturn(List.of(zFile, aFile));

        List<FileResponse> responses = fileService.getFileTree(5L);

        assertThat(responses)
                .extracting(FileResponse::getPath)
                .containsExactly("alpha/Main.java", "zeta/Main.java");
    }

    @Test
    void getFileByIdThrowsWhenActiveFileDoesNotExist() {
        when(repository.findByFileIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileById(404L))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessage("Active file not found with id: 404");
    }

    private ProjectFile activeFile(Long fileId, String path, String content) {
        return ProjectFile.builder()
                .fileId(fileId)
                .projectId(5L)
                .name(path.substring(path.lastIndexOf("/") + 1))
                .path(path)
                .language("Java")
                .content(content)
                .size((long) content.length())
                .folder(false)
                .createdById(7L)
                .lastEditedBy(7L)
                .deleted(false)
                .build();
    }
}
