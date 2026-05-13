package com.app.commentservice.service;

import com.app.commentservice.dto.*;

import java.util.List;

public interface CommentService {

    CommentResponse addComment(AddCommentRequest request);

    List<CommentResponse> getByFile(Long fileId);

    List<CommentResponse> getByProject(Long projectId);

    CommentResponse getCommentById(Long commentId);

    List<CommentResponse> getReplies(Long commentId);

    CommentResponse updateComment(Long commentId, UpdateCommentRequest request);

    void deleteComment(Long commentId);

    CommentResponse resolveComment(Long commentId);

    CommentResponse unresolveComment(Long commentId);

    List<CommentResponse> getByLine(Long fileId, Integer lineNumber);

    CommentCountResponse getCommentCount(Long fileId);
}