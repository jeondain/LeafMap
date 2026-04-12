package com.fromm.leafmap.domain.post.repository;

import com.fromm.leafmap.domain.post.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {
}
