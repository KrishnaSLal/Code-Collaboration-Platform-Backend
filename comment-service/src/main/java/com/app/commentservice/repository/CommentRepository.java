package com.app.commentservice.repository;

import com.app.commentservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByFileId(Long fileId);

    List<Comment> findByProjectId(Long projectId);

    List<Comment> findByAuthorId(Long authorId);

    List<Comment> findByLineNumber(Integer lineNumber);

    List<Comment> findByFileIdAndLineNumber(Long fileId, Integer lineNumber);

    List<Comment> findByParentCommentId(Long parentCommentId);

    long countByFileId(Long fileId);

    List<Comment> findByResolved(Boolean resolved);

    Optional<Comment> findByCommentId(Long commentId);

    void deleteByCommentId(Long commentId);
}