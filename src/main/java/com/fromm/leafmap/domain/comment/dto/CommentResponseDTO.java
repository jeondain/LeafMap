package com.fromm.leafmap.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CommentResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDTO {
        private Long id;
        private String content;
        private String nickname;
        private Boolean isWriter;
        private LocalDateTime createdAt;
    }
}
