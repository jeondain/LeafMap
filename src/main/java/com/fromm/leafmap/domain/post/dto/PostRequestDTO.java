package com.fromm.leafmap.domain.post.dto;

import lombok.*;

public class PostRequestDTO {

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPostRequestDTO {
        private String title;
        private String content;
        private String address;
    }
}
