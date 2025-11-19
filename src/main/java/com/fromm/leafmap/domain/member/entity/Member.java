package com.fromm.leafmap.domain.member.entity;

import com.fromm.leafmap.domain.major.entity.Major;
import com.fromm.leafmap.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long id;

    private String loginId;

    private String password;

    private String nickname;

    private String studentId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desired_major_id")
    private Major desiredMajor;
}
