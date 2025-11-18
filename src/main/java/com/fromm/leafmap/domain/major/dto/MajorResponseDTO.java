package com.fromm.leafmap.domain.major.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MajorResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MajorDTO {
        private Long id;
        private String name;
        private String keywords;
        private String curriculumUrl;
        private String description;
        private String career;;
    }
}
