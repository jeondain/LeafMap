package com.fromm.leafmap.domain.aptitude.entity;

import com.fromm.leafmap.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class AptitudeOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aptitude_option_id", nullable = false)
    private Long id;

    private String content;

    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aptitude_question_id")
    private AptitudeQuestion aptitudeQuestion;
}
