package com.app.commentservice.controller;

import com.app.commentservice.dto.*;
import com.app.commentservice.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public CommentResponse addComment(@RequestBody AddCommentRequest request) {
        return commentService.addComment(request);
    }

    @GetMapping("/file/{fileId}")
    public List<CommentResponse> getByFile(@PathVariable Long fileId) {
        return commentService.getByFile(fileId);
    }

    @GetMapping("/project/{projectId}")
    public List<CommentResponse> getByProject(@PathVariable Long projectId) {
        return commentService.getByProject(projectId);
    }

    @GetMapping("/{commentId}")
    public CommentResponse getCommentById(@PathVariable Long commentId) {
        return commentService.getCommentById(commentId);
    }

    @GetMapping("/{commentId}/replies")
    public List<CommentResponse> getReplies(@PathVariable Long commentId) {
        return commentService.getReplies(commentId);
    }

    @PutMapping("/{commentId}")
    public CommentResponse updateComment(@PathVariable Long commentId,
                                         @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public String deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return "Comment deleted successfully";
    }

    @PutMapping("/{commentId}/resolve")
    public CommentResponse resolveComment(@PathVariable Long commentId) {
        return commentService.resolveComment(commentId);
    }

    @PutMapping("/{commentId}/unresolve")
    public CommentResponse unresolveComment(@PathVariable Long commentId) {
        return commentService.unresolveComment(commentId);
    }

    @GetMapping("/file/{fileId}/line/{lineNumber}")
    public List<CommentResponse> getByLine(@PathVariable Long fileId,
                                           @PathVariable Integer lineNumber) {
        return commentService.getByLine(fileId, lineNumber);
    }

    @GetMapping("/file/{fileId}/count")
    public CommentCountResponse getCommentCount(@PathVariable Long fileId) {
        return commentService.getCommentCount(fileId);
    }
}