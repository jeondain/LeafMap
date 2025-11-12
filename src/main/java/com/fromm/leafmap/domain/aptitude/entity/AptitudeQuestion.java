package com.fromm.leafmap.domain.aptitude.entity;

import com.fromm.leafmap.domain.major.entity.MajorCategory;
import com.fromm.leafmap.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class AptitudeQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aptitude_question_id", nullable = false)
    private Long id;

    private String content;

    private float weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_category_id")
    private MajorCategory majorCategory;

    @OneToMany(mappedBy = "aptitudeQuestion", cascade = CascadeType.ALL)
    private List<AptitudeOption> aptitudeOptions = new ArrayList<>();
}
