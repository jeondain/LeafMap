package com.fromm.leafmap.domain.chatbot.dto;

import com.fromm.leafmap.domain.post.entity.BoardType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResultDTO {
        private String message;
        private List<PostPreviewDTO> posts;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostPreviewDTO {
        private Long postId;
        private BoardType boardType;
        private String title;
        private String contentPreview;
        private String address;
        private String imageUrl;
        private Boolean badge;
        private Integer likeCount;
        private LocalDateTime createdAt;
    }
}