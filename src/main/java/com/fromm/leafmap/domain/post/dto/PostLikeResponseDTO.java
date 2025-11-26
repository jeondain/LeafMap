package com.fromm.leafmap.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PostLikeResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostLikeResultDTO {
        private Long postId;
        private Integer likeCount;
        private Boolean isLiked;
    }
}
