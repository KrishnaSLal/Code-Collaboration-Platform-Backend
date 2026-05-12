package com.app.commentservice.service;

import com.app.commentservice.dto.*;
import com.app.commentservice.entity.Comment;
import com.app.commentservice.exception.CommentNotFoundException;
import com.app.commentservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository repository;

    @Override
    public CommentResponse addComment(AddCommentRequest request) {
        Comment comment = Comment.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .authorId(request.getAuthorId())
                .content(request.getContent())
                .lineNumber(request.getLineNumber())
                .columnNumber(request.getColumnNumber())
                .parentCommentId(request.getParentCommentId())
                .resolved(false)
                .snapshotId(request.getSnapshotId())
                .build();

        return mapToResponse(repository.save(comment));
    }

    @Override
    public List<CommentResponse> getByFile(Long fileId) {
        return repository.findByFileId(fileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentResponse> getByProject(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        return mapToResponse(fetchComment(commentId));
    }

    @Override
    public List<CommentResponse> getReplies(Long commentId) {
        fetchComment(commentId);
        return repository.findByParentCommentId(commentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request) {
        Comment comment = fetchComment(commentId);
        comment.setContent(request.getContent());
        return mapToResponse(repository.save(comment));
    }

    @Override
    public void deleteComment(Long commentId) {
        fetchComment(commentId);
        repository.deleteByCommentId(commentId);
    }

    @Override
    public CommentResponse resolveComment(Long commentId) {
        Comment comment = fetchComment(commentId);
        comment.setResolved(true);
        return mapToResponse(repository.save(comment));
    }

    @Override
    public CommentResponse unresolveComment(Long commentId) {
        Comment comment = fetchComment(commentId);
        comment.setResolved(false);
        return mapToResponse(repository.save(comment));
    }

    @Override
    public List<CommentResponse> getByLine(Long fileId, Integer lineNumber) {
        return repository.findByFileIdAndLineNumber(fileId, lineNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentCountResponse getCommentCount(Long fileId) {
        long count = repository.countByFileId(fileId);
        return CommentCountResponse.builder()
                .fileId(fileId)
                .count(count)
                .build();
    }

    private Comment fetchComment(Long commentId) {
        return repository.findByCommentId(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .projectId(comment.getProjectId())
                .fileId(comment.getFileId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .lineNumber(comment.getLineNumber())
                .columnNumber(comment.getColumnNumber())
                .parentCommentId(comment.getParentCommentId())
                .resolved(comment.getResolved())
                .snapshotId(comment.getSnapshotId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}