package com.fromm.leafmap.domain.aptitude.service;

import com.fromm.leafmap.domain.aptitude.dto.AptitudeRequestDTO;
import com.fromm.leafmap.domain.aptitude.dto.AptitudeResponseDTO;
import com.fromm.leafmap.domain.aptitude.entity.AptitudeQuestion;
import com.fromm.leafmap.domain.aptitude.entity.MajorQuestionWeight;
import com.fromm.leafmap.domain.aptitude.repository.AptitudeQuestionRepository;
import com.fromm.leafmap.domain.aptitude.repository.MajorQuestionWeightRepository;
import com.fromm.leafmap.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AptitudeServiceImpl implements AptitudeService{

    private final AptitudeQuestionRepository aptitudeRepository;
    private final MajorQuestionWeightRepository weightRepository;

    @Override
    @Transactional
    public AptitudeResponseDTO.GetQuestionsResultDTO getQuestions(Member member) {

        List<AptitudeQuestion> questions = aptitudeRepository.findAllByOrderByIdAsc();

        List<AptitudeResponseDTO.QuestionDTO> questionDTOs = questions.stream()
                .map(q -> AptitudeResponseDTO.QuestionDTO.builder()
                        .questionId(q.getId())
                        .content(q.getContent())
                        .build())
                .toList();

        return AptitudeResponseDTO.GetQuestionsResultDTO.builder()
                .questions(questionDTOs)
                .build();
    }

    @Override
    @Transactional
    public AptitudeResponseDTO.GetAptitudeResultDTO calculateResult(AptitudeRequestDTO.SubmitAnswerDTO request) {

        List<MajorQuestionWeight> weights = weightRepository.findAll();

        Map<Long, List<MajorQuestionWeight>> weightMap = weights.stream()
                .collect(Collectors.groupingBy(w -> w.getAptitudeQuestion().getId()));

        Map<String, Double> majorScores = request.getAnswers().stream()
                .flatMap(ans -> weightMap.getOrDefault(ans.getQuestionId(), List.of()).stream()
                        .map(w -> Map.entry(w.getMajor().getName(), ans.getScore() * w.getWeight())))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingDouble(Map.Entry::getValue)));

        // 최고 점수 전공만 추출
        Map.Entry<String, Double> topMajor = majorScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new IllegalStateException("점수 계산 결과가 없습니다."));

        Map<String, Double> topMajorMap = Map.of(topMajor.getKey(), topMajor.getValue());

        return AptitudeResponseDTO.GetAptitudeResultDTO.builder()
                .majorScores(topMajorMap)
                .build();
    }
}

