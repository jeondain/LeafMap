package com.fromm.leafmap.domain.aptitude.controller;

import com.fromm.leafmap.domain.aptitude.dto.AptitudeResponseDTO;
import com.fromm.leafmap.domain.aptitude.service.AptitudeService;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.global.annotation.CurrentMember;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aptitude-test")
public class AptitudeController {

    private final AptitudeService aptitudeService;

    @GetMapping
    @Operation(summary = "전공 적성 검사 문항 조회")
    public ApiResponse<AptitudeResponseDTO.GetQuestionsResultDTO> getQuestions(
            @CurrentMember Member member) {

        AptitudeResponseDTO.GetQuestionsResultDTO getQuestionsResultDTO = aptitudeService.getQuestions(member);
        return ApiResponse.onSuccess(getQuestionsResultDTO);
    }
}
