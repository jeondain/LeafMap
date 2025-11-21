package com.fromm.leafmap.domain.post.dto;

import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.major.dto.MajorResponseDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPostResultDTO {
        private Long postId;
    }

    @Getter
    @Builder
    public static class PostPreviewDTO {
        private Long postId;
        private String title;
        private String contentPreview; // 내용 첫 줄
        private Boolean isPublic;

        private Long majorId;
        private String majorName;

        private BoardType boardType;

        private String authorInfo;
    }

    @Getter
    @Builder
    public static class PostListResultDTO {
        private List<PostPreviewDTO> posts;
        private Long nextCursor;
        private boolean hasNext;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostDetailResultDTO {

        private Long postId;
        private BoardType boardType;
        private String title;
        private String content;
        private String address;
        private String imageUrl;
        private Boolean isPublic;
        private Integer likeCount;
        private Boolean badge;
        private Boolean isWriter;
        private Boolean isLiked;
        private String authorInfo;

        private MemberResponseDTO.MemberDTO member;
        private MajorResponseDTO.MajorDTO major;

        private List<CommentResponseDTO.CommentDTO> comments;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantPostPreviewDTO {
        private Long postId;
        private String imageUrl;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantPostListResultDTO {
        private List<RestaurantPostPreviewDTO> posts;
    }
}
