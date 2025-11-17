package com.fromm.leafmap.domain.post.repository;

import com.fromm.leafmap.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
