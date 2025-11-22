package com.fromm.leafmap.domain.major.entity;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.entity.Post;
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

    @OneToOne(mappedBy = "major", fetch = FetchType.LAZY)
    private Post post;

    public void setCurriculumUrl(String curriculumUrl) {
        this.curriculumUrl = curriculumUrl;
    }
}
