package com.app.commentservice.service;

import com.app.commentservice.dto.AddCommentRequest;
import com.app.commentservice.dto.CommentCountResponse;
import com.app.commentservice.dto.CommentResponse;
import com.app.commentservice.dto.UpdateCommentRequest;
import com.app.commentservice.entity.Comment;
import com.app.commentservice.exception.CommentNotFoundException;
import com.app.commentservice.repository.CommentRepository;
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
class CommentServiceImplTest {

    @Mock
    private CommentRepository repository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void addCommentCreatesUnresolvedComment() {
        AddCommentRequest request = AddCommentRequest.builder()
                .projectId(1L)
                .fileId(2L)
                .authorId(3L)
                .content("Please rename this variable")
                .lineNumber(12)
                .columnNumber(4)
                .snapshotId("snapshot-1")
                .build();

        when(repository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId(100L);
            return comment;
        });

        CommentResponse response = commentService.addComment(request);

        assertThat(response.getCommentId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getFileId()).isEqualTo(2L);
        assertThat(response.getResolved()).isFalse();
        assertThat(response.getContent()).isEqualTo("Please rename this variable");
    }

    @Test
    void updateCommentChangesContent() {
        Comment comment = comment(10L, "old text");
        when(repository.findByCommentId(10L)).thenReturn(Optional.of(comment));
        when(repository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.updateComment(10L, UpdateCommentRequest.builder()
                .content("new text")
                .build());

        assertThat(response.getContent()).isEqualTo("new text");
    }

    @Test
    void getByFileReturnsCommentsForFile() {
        when(repository.findByFileId(2L)).thenReturn(List.of(comment(10L, "file comment")));

        List<CommentResponse> responses = commentService.getByFile(2L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFileId()).isEqualTo(2L);
    }

    @Test
    void getByProjectReturnsCommentsForProject() {
        when(repository.findByProjectId(1L)).thenReturn(List.of(comment(10L, "project comment")));

        List<CommentResponse> responses = commentService.getByProject(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void resolveCommentMarksCommentResolved() {
        Comment comment = comment(10L, "text");
        comment.setResolved(false);
        when(repository.findByCommentId(10L)).thenReturn(Optional.of(comment));
        when(repository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.resolveComment(10L);

        assertThat(response.getResolved()).isTrue();
    }

    @Test
    void unresolveCommentMarksCommentUnresolved() {
        Comment comment = comment(10L, "text");
        comment.setResolved(true);
        when(repository.findByCommentId(10L)).thenReturn(Optional.of(comment));
        when(repository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.unresolveComment(10L);

        assertThat(response.getResolved()).isFalse();
    }

    @Test
    void getRepliesRequiresParentCommentToExist() {
        Comment parent = comment(10L, "parent");
        Comment reply = comment(11L, "reply");
        reply.setParentCommentId(10L);

        when(repository.findByCommentId(10L)).thenReturn(Optional.of(parent));
        when(repository.findByParentCommentId(10L)).thenReturn(List.of(reply));

        List<CommentResponse> replies = commentService.getReplies(10L);

        assertThat(replies).hasSize(1);
        assertThat(replies.get(0).getParentCommentId()).isEqualTo(10L);
    }

    @Test
    void getCommentCountReturnsRepositoryCount() {
        when(repository.countByFileId(2L)).thenReturn(3L);

        CommentCountResponse response = commentService.getCommentCount(2L);

        assertThat(response.getFileId()).isEqualTo(2L);
        assertThat(response.getCount()).isEqualTo(3L);
    }

    @Test
    void getByLineReturnsCommentsForFileLine() {
        when(repository.findByFileIdAndLineNumber(2L, 12))
                .thenReturn(List.of(comment(10L, "line comment")));

        List<CommentResponse> responses = commentService.getByLine(2L, 12);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLineNumber()).isEqualTo(12);
    }

    @Test
    void deleteCommentDelegatesAfterFetch() {
        when(repository.findByCommentId(10L)).thenReturn(Optional.of(comment(10L, "text")));

        commentService.deleteComment(10L);

        verify(repository).deleteByCommentId(10L);
    }

    @Test
    void getCommentByIdThrowsWhenMissing() {
        when(repository.findByCommentId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById(404L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage("Comment not found with id: 404");
    }

    private Comment comment(Long id, String content) {
        return Comment.builder()
                .commentId(id)
                .projectId(1L)
                .fileId(2L)
                .authorId(3L)
                .content(content)
                .lineNumber(12)
                .columnNumber(4)
                .resolved(false)
                .snapshotId("snapshot-1")
                .build();
    }
}
