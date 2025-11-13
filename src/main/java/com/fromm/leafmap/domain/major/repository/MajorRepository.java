package com.fromm.leafmap.domain.major.repository;

import com.fromm.leafmap.domain.major.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByName(String major);
}
