package com.fromm.leafmap.domain.post.repository;

import com.fromm.leafmap.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
