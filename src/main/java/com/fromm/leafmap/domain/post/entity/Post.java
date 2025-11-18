package com.fromm.leafmap.domain.post.entity;

import com.fromm.leafmap.domain.comment.entity.Comment;
import com.fromm.leafmap.domain.major.entity.Major;
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
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type")
    private BoardType boardType;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    private String address;

    private String imageUrl;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "badge")
    private Boolean badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostLike> likes = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "major_id")
    private Major major;
}
