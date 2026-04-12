package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.post.document.PostDocument;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.domain.post.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostIndexService implements ApplicationRunner {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;

    /**
     * 전체 재인덱싱. 서버 최초 세팅이나 인덱스 초기화 시 사용.
     */
    @Transactional(readOnly = true)
    public void reindexAll() {
        List<Post> allPosts = postRepository.findAllByIsPublicTrue();
        List<PostDocument> documents = allPosts.stream()
            .map(this::toDocument)
            .toList();
        postSearchRepository.saveAll(documents);
        log.info("ES 재인덱싱 완료: {}건", documents.size());
    }

    /**
     * 서버 시작 시 자동으로 1회 전체 인덱싱 수행.
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            reindexAll();
        } catch (Exception e) {
            log.warn("ES 초기 인덱싱 실패 (ES 미연결 상태일 수 있음): {}", e.getMessage());
        }
    }

    public PostDocument toDocument(Post post) {
        return PostDocument.builder()
            .id(post.getId())
            .boardType(post.getBoardType().name())
            .title(post.getTitle())
            .content(post.getContent())
            .address(post.getAddress())
            .badge(post.getBadge())
            .likeCount(post.getLikeCount())
            .createdAt(post.getCreatedAt())
            .imageUrl(post.getImageUrl())
            .build();
    }
}
