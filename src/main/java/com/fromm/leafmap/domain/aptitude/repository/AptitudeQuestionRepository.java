package com.fromm.leafmap.domain.aptitude.repository;

import com.fromm.leafmap.domain.aptitude.entity.AptitudeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AptitudeQuestionRepository extends JpaRepository<AptitudeQuestion, Long> {
    List<AptitudeQuestion> findAllByOrderByIdAsc();
}
