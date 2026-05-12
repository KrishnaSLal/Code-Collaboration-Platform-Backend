package com.app.commentservice.controller;

import com.app.commentservice.dto.AddCommentRequest;
import com.app.commentservice.dto.CommentCountResponse;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.UpdateCommentRequest;
import com.app.commentservice.service.CommentService;
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
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private CommentController controller;

    @BeforeEach
    void setUp() {
        controller = new CommentController(commentService);
    }

    @Test
    void delegatesCommentOperations() {
        AddCommentRequest addRequest = AddCommentRequest.builder().content("Looks good").build();
        UpdateCommentRequest updateRequest = UpdateCommentRequest.builder().content("Updated").build();
        CommentResponse response = CommentResponse.builder().commentId(1L).content("Looks good").build();
        CommentCountResponse count = CommentCountResponse.builder().fileId(2L).count(3L).build();

        when(commentService.addComment(addRequest)).thenReturn(response);
        when(commentService.getByFile(2L)).thenReturn(List.of(response));
        when(commentService.getByProject(5L)).thenReturn(List.of(response));
        when(commentService.getCommentById(1L)).thenReturn(response);
        when(commentService.getReplies(1L)).thenReturn(List.of(response));
        when(commentService.updateComment(1L, updateRequest)).thenReturn(response);
        when(commentService.resolveComment(1L)).thenReturn(response);
        when(commentService.unresolveComment(1L)).thenReturn(response);
        when(commentService.getByLine(2L, 10)).thenReturn(List.of(response));
        when(commentService.getCommentCount(2L)).thenReturn(count);

        assertThat(controller.addComment(addRequest)).isSameAs(response);
        assertThat(controller.getByFile(2L)).containsExactly(response);
        assertThat(controller.getByProject(5L)).containsExactly(response);
        assertThat(controller.getCommentById(1L)).isSameAs(response);
        assertThat(controller.getReplies(1L)).containsExactly(response);
        assertThat(controller.updateComment(1L, updateRequest)).isSameAs(response);
        assertThat(controller.deleteComment(1L)).isEqualTo("Comment deleted successfully");
        assertThat(controller.resolveComment(1L)).isSameAs(response);
        assertThat(controller.unresolveComment(1L)).isSameAs(response);
        assertThat(controller.getByLine(2L, 10)).containsExactly(response);
        assertThat(controller.getCommentCount(2L)).isSameAs(count);

        verify(commentService).deleteComment(1L);
    }
}
