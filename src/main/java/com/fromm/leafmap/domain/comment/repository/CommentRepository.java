package com.fromm.leafmap.domain.comment.repository;

import com.fromm.leafmap.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
