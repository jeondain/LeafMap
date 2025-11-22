package com.fromm.leafmap.domain.aptitude.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AptitudeRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerDTO {
        private List<AnswerDTO> answers;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDTO {
        private Long questionId;
        private int score;
    }
}
