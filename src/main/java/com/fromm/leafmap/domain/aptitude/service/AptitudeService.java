package com.fromm.leafmap.domain.aptitude.service;

import com.fromm.leafmap.domain.aptitude.dto.AptitudeResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface AptitudeService {
    AptitudeResponseDTO.GetQuestionsResultDTO getQuestions(Member member);
}
