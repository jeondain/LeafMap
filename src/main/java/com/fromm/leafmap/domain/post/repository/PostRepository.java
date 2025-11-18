package com.fromm.leafmap.domain.post.repository;

import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByMajorId(Long majorId);

    @Query("SELECT p FROM Post p " +
            "WHERE p.boardType = :boardType " +
            "AND p.id < :cursor " +
            "AND p.isPublic = true " +
            "ORDER BY p.id DESC")
    List<Post> findPostList(@Param("boardType") BoardType boardType, @Param("cursor") Long cursor, Pageable pageable);
}
