package com.fromm.leafmap.domain.major.entity;

import com.fromm.leafmap.domain.member.entity.Member;
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
public class Major extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "major_id", nullable = false)
    private Long id;

    private String name;

    private String keywords;

    private String curriculumUrl;

    private String description;

    private String career;

    @OneToMany(mappedBy = "major", cascade = CascadeType.ALL)
    private List<Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "desiredMajor")
    private List<Member> interestedMembers = new ArrayList<>();

    @OneToMany(mappedBy = "major", cascade = CascadeType.ALL)
    private List<MajorCategoryMap> majorCategoryMaps = new ArrayList<>();

    public void setCurriculumUrl(String curriculumUrl) {
        this.curriculumUrl = curriculumUrl;
    }
}
