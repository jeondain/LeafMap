package com.fromm.leafmap.domain.post.repository;

import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByMajorId(Long majorId);

    Optional<Post> findByIdAndBoardType(Long id, BoardType boardType);

    List<Post> findTop10ByBoardTypeAndIsPublicTrueOrderByCreatedAtDesc(BoardType boardType);

    @Query("SELECT p FROM Post p " +
            "WHERE p.boardType = :boardType " +
            "AND p.id < :cursor " +
            "AND p.isPublic = true " +
            "ORDER BY p.id DESC")
    List<Post> findPostList(@Param("boardType") BoardType boardType, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "WHERE p.member.id = :memberId " +
            "AND p.id < :cursor " +
            "ORDER BY p.id DESC")
    List<Post> findMyPostList(@Param("memberId") Long memberId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "JOIN PostLike pl ON pl.post = p " +
            "WHERE pl.member.id = :memberId " +
            "AND p.id < :cursor " +
            "ORDER BY p.id DESC")
    List<Post> findPostsLikedByMember(@Param("memberId") Long memberId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT DISTINCT c.post FROM Comment c " +
            "WHERE c.member.id = :memberId " +
            "AND c.post.id < :cursor " +
            "ORDER BY c.post.id DESC")
    List<Post> findPostsCommentedByMember(@Param("memberId") Long memberId, @Param("cursor") Long cursor, Pageable pageable);

    @Query(value =
            "SELECT * FROM post p " +
                    "WHERE p.is_public = true " +
                    "AND (:boardType IS NULL OR p.board_type = :boardType) " +
                    "AND (:address IS NULL OR " +
                    "     p.address LIKE CONCAT('%', :address, '%') OR " +
                    "     p.title LIKE CONCAT('%', :address, '%') OR " +
                    "     p.content LIKE CONCAT('%', :address, '%')) " +
                    "AND (:hasBadge IS NULL OR p.badge = :hasBadge) " +
                    "AND (:keyword IS NULL OR " +
                    "     p.title REGEXP REPLACE(:keyword, ' ', '|') OR " +
                    "     p.content REGEXP REPLACE(:keyword, ' ', '|')) " +
                    "ORDER BY p.created_at DESC",
            nativeQuery = true)
    List<Post> searchPostsByCondition(
            @Param("boardType") String boardType,
            @Param("address") String address,
            @Param("hasBadge") Boolean hasBadge,
            @Param("keyword") String keyword
    );
}
