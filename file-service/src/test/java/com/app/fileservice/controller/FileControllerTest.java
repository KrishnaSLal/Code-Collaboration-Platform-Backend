package com.app.fileservice.controller;

import com.app.fileservice.dto.FileContentUpdateRequest;
import com.app.fileservice.dto.FileRequest;
import com.app.fileservice.dto.FileResponse;
import com.app.fileservice.dto.FolderRequest;
import com.app.fileservice.dto.MoveFileRequest;
import com.app.fileservice.dto.RenameFileRequest;
import com.app.fileservice.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    private FileController controller;

    @BeforeEach
    void setUp() {
        controller = new FileController(fileService);
    }

    @Test
    void delegatesFileOperations() {
        FileRequest fileRequest = FileRequest.builder().name("Main.java").build();
        FolderRequest folderRequest = FolderRequest.builder().name("src").build();
        FileContentUpdateRequest contentRequest = FileContentUpdateRequest.builder().content("class Main {}").build();
        RenameFileRequest renameRequest = RenameFileRequest.builder().newName("App.java").build();
        MoveFileRequest moveRequest = MoveFileRequest.builder().newPath("app/App.java").build();
        FileResponse response = FileResponse.builder().fileId(1L).name("Main.java").build();

        when(fileService.createFile(fileRequest)).thenReturn(response);
        when(fileService.createFolder(folderRequest)).thenReturn(response);
        when(fileService.getFileById(1L)).thenReturn(response);
        when(fileService.getFilesByProject(5L)).thenReturn(List.of(response));
        when(fileService.getFileContent(1L)).thenReturn("content");
        when(fileService.updateFileContent(1L, contentRequest)).thenReturn(response);
        when(fileService.renameFile(1L, renameRequest)).thenReturn(response);
        when(fileService.moveFile(1L, moveRequest)).thenReturn(response);
        when(fileService.restoreFile(1L)).thenReturn(response);
        when(fileService.getFileTree(5L)).thenReturn(List.of(response));
        when(fileService.searchInProject(5L, "main")).thenReturn(List.of(response));

        assertThat(controller.createFile(fileRequest)).isSameAs(response);
        assertThat(controller.createFolder(folderRequest)).isSameAs(response);
        assertThat(controller.getFileById(1L)).isSameAs(response);
        assertThat(controller.getFilesByProject(5L)).containsExactly(response);
        assertThat(controller.getFileContent(1L)).isEqualTo("content");
        assertThat(controller.updateFileContent(1L, contentRequest)).isSameAs(response);
        assertThat(controller.renameFile(1L, renameRequest)).isSameAs(response);
        assertThat(controller.moveFile(1L, moveRequest)).isSameAs(response);
        assertThat(controller.deleteFile(1L)).isEqualTo("File deleted successfully");
        assertThat(controller.restoreFile(1L)).isSameAs(response);
        assertThat(controller.getFileTree(5L)).containsExactly(response);
        assertThat(controller.searchInProject(5L, "main")).containsExactly(response);

        verify(fileService).deleteFile(1L);
    }
}
