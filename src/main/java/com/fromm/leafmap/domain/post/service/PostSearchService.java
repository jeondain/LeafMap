package com.fromm.leafmap.domain.post.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.fromm.leafmap.domain.post.document.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public List<PostDocument> search(String boardType, String address,
                                     Boolean hasBadge, String keyword) {

        List<Query> filters = new ArrayList<>();
        List<Query> musts = new ArrayList<>();
        List<Query> shoulds = new ArrayList<>();

        // boardType 필터
        if (boardType != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("boardType").value(boardType))));
        }

        // badge 필터
        if (hasBadge != null && hasBadge) {
            filters.add(Query.of(q -> q.term(t -> t.field("badge").value(true))));
        }

        // keyword 검색
        if (keyword != null && !keyword.isBlank()) {
            final String kw = keyword;
            musts.add(Query.of(q -> q.multiMatch(m -> m
                .fields("title", "content")
                .query(kw)
                .type(TextQueryType.MostFields)
                .fuzziness("1")
                .minimumShouldMatch("75%")
            )));
        }

        // address 검색 (should로 부스팅)
        if (address != null && !address.isBlank()) {
            final String addr = address;
            shoulds.add(Query.of(q -> q.match(m -> m.field("address").query(addr).boost(2.0f))));
            shoulds.add(Query.of(q -> q.multiMatch(m -> m
                .fields("title", "content")
                .query(addr)
                .boost(0.5f)
            )));
        }

        Query boolQuery = Query.of(q -> q.bool(b -> {
            if (!filters.isEmpty()) b.filter(filters);
            if (!musts.isEmpty()) b.must(musts);
            if (!shoulds.isEmpty()) b.should(shoulds);
            return b;
        }));

        NativeQuery searchQuery = NativeQuery.builder()
            .withQuery(boolQuery)
            .withSort(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))))
            .withSort(SortOptions.of(s -> s.field(f -> f.field("likeCount").order(SortOrder.Desc))))
            .withSort(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))))
            .withMaxResults(10)
            .build();

        SearchHits<PostDocument> hits = elasticsearchOperations.search(searchQuery, PostDocument.class);

        return hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList();
    }
}
