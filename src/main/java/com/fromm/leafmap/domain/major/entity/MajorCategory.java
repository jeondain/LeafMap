package com.fromm.leafmap.domain.major.entity;

import com.fromm.leafmap.domain.aptitude.entity.AptitudeQuestion;
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
public class MajorCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "major_category_id", nullable = false)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "majorCategory", cascade = CascadeType.ALL)
    private List<MajorCategoryMap> majorCategoryMaps = new ArrayList<>();

    @OneToMany(mappedBy = "majorCategory", cascade = CascadeType.ALL)
    private List<AptitudeQuestion> aptitudeQuestions = new ArrayList<>();
}
