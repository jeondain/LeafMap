package com.fromm.leafmap.domain.aptitude.service;

import com.fromm.leafmap.domain.aptitude.dto.AptitudeResponseDTO;
import com.fromm.leafmap.domain.aptitude.entity.AptitudeQuestion;
import com.fromm.leafmap.domain.aptitude.repository.AptitudeQuestionRepository;
import com.fromm.leafmap.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AptitudeServiceImpl implements AptitudeService{

    private final AptitudeQuestionRepository aptitudeRepository;

    @Override
    @Transactional
    public AptitudeResponseDTO.GetQuestionsResultDTO getQuestions(Member member) {

        List<AptitudeQuestion> questions = aptitudeRepository.findAllByOrderByIdAsc();

        List<AptitudeResponseDTO.QuestionDTO> questionDTOs = questions.stream()
                .map(q -> AptitudeResponseDTO.QuestionDTO.builder()
                        .questionID(q.getId())
                        .content(q.getContent())
                        .build())
                .toList();

        return AptitudeResponseDTO.GetQuestionsResultDTO.builder()
                .questions(questionDTOs)
                .build();
    }
}

