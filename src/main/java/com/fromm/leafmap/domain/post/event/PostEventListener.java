package com.fromm.leafmap.domain.post.event;

import com.fromm.leafmap.domain.post.repository.PostSearchRepository;
import com.fromm.leafmap.domain.post.service.PostIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final PostSearchRepository postSearchRepository;
    private final PostIndexService postIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        postSearchRepository.save(postIndexService.toDocument(event.getPost()));
        log.debug("ES 인덱싱 - 생성: postId={}", event.getPost().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostUpdated(PostUpdatedEvent event) {
        postSearchRepository.save(postIndexService.toDocument(event.getPost()));
        log.debug("ES 인덱싱 - 수정: postId={}", event.getPost().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeleted(PostDeletedEvent event) {
        postSearchRepository.deleteById(event.getPostId());
        log.debug("ES 인덱싱 - 삭제: postId={}", event.getPostId());
    }
}
