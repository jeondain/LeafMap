package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.major.entity.Major;
import com.fromm.leafmap.domain.major.repository.MajorRepository;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajortipsPostInitService {

    private final PostRepository postRepository;
    private final MajorRepository majorRepository;

    @Transactional
    public void createPostsFromMajors() {
        List<Major> majors = majorRepository.findAll();

        for (Major major : majors) {

            if ("미래융합학부1".equals(major.getName())
                    || "미래융합학부2".equals(major.getName())
                    || "자유전공학부".equals(major.getName())) {
                continue;
            }

            boolean exists = postRepository.existsByMajorId(major.getId());
            if (exists) continue;

            Post post = Post.builder()
                    .title(major.getName())
                    .content(major.getDescription())
                    .isPublic(true)
                    .boardType(BoardType.MAJOR_TIPS)
                    .major(major)
                    .build();

            postRepository.save(post);
        }
    }
}
