package com.fromm.leafmap.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class PostResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPostResultDTO {
        private Long id;
    }

    @Getter
    @Builder
    public static class PostPreviewDTO {
        private Long postId;
        private String title;
        private String contentPreview; // 내용 첫 줄

        private Long majorId;
        private String majorName;
    }

    @Getter
    @Builder
    public static class PostListResultDTO {
        private List<PostPreviewDTO> posts;
        private Long nextCursor;
        private boolean hasNext;
    }
}
