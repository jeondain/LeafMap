package com.fromm.leafmap.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromm.leafmap.domain.chatbot.config.OpenAIClient;
import com.fromm.leafmap.domain.chatbot.dto.ChatRequestDTO;
import com.fromm.leafmap.domain.chatbot.dto.ChatResponseDTO;
import com.fromm.leafmap.domain.chatbot.dto.SearchCondition;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.document.PostDocument;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final PostSearchService postSearchService;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResponseDTO.ChatResultDTO processQuery(
            ChatRequestDTO.ChatQueryDTO request, Member member) {

        // 1. AI로 자연어 → 검색 조건 파싱
        SearchCondition condition = parseWithAI(request.getMessage());

        log.info("파싱 결과 - boardType: {}, address: {}, hasBadge: {}, keyword: {}",
                condition.getBoardType(), condition.getAddress(),
                condition.getHasBadge(), condition.getKeyword());

        // 2. Elasticsearch 검색
        List<PostDocument> searchResults = postSearchService.search(
                condition.getBoardType() != null ? condition.getBoardType().name() : null,
                condition.getAddress(),
                condition.getHasBadge(),
                condition.getKeyword()
        );

        log.info("검색 결과: {}개", searchResults.size());

        // 3. RAG 응답 생성
        String aiResponse = generateRAGResponse(request.getMessage(), searchResults);

        // 4. 게시글 프리뷰 (상위 5개)
        List<ChatResponseDTO.PostPreviewDTO> postPreviews = searchResults.stream()
                .limit(5)
                .map(doc -> ChatResponseDTO.PostPreviewDTO.builder()
                        .postId(doc.getId())
                        .boardType(BoardType.valueOf(doc.getBoardType()))
                        .title(doc.getTitle())
                        .contentPreview(extractFirstLine(doc.getContent()))
                        .address(doc.getAddress())
                        .imageUrl(doc.getImageUrl())
                        .badge(doc.getBadge())
                        .likeCount(doc.getLikeCount())
                        .createdAt(doc.getCreatedAt())
                        .build())
                .toList();

        return ChatResponseDTO.ChatResultDTO.builder()
                .message(aiResponse)
                .posts(postPreviews)
                .build();
    }

    // ===================== AI 파싱 =====================

    private SearchCondition parseWithAI(String message) {
        String systemPrompt = """
                너는 대학 캠퍼스 정보 검색 쿼리 파서야.
                사용자의 자연어 질문을 분석해서 JSON으로만 응답해.

                필드:
                - boardType: RESTAURANT | SHORTCUTS | FACILITY_USAGE | MAJOR_TIPS | CAMPUS_LIFE_TIPS | null
                - address: 지역/장소명 (없으면 null)
                - hasBadge: 뱃지/배지/인증 언급 시 true, 아니면 null
                - keyword: 핵심 검색 키워드 (자연어 그대로, 1~5단어)
                """;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // Few-shot 예시
        messages.add(Map.of("role", "user", "content", "성신여대 근처 카페 추천해줘"));
        messages.add(Map.of("role", "assistant", "content",
                "{\"boardType\":\"RESTAURANT\",\"address\":\"성신\",\"hasBadge\":null,\"keyword\":\"카페\"}"));

        messages.add(Map.of("role", "user", "content", "도서관 몇 시까지 열어?"));
        messages.add(Map.of("role", "assistant", "content",
                "{\"boardType\":\"FACILITY_USAGE\",\"address\":null,\"hasBadge\":null,\"keyword\":\"도서관 운영시간\"}"));

        messages.add(Map.of("role", "user", "content", "컴공과 졸업하면 뭐 할 수 있어?"));
        messages.add(Map.of("role", "assistant", "content",
                "{\"boardType\":\"MAJOR_TIPS\",\"address\":null,\"hasBadge\":null,\"keyword\":\"컴퓨터공학 졸업 진로\"}"));

        // 실제 질문
        messages.add(Map.of("role", "user", "content", message));

        try {
            String json = openAIClient.chat(messages, 0.1, 150, true);
            return parseSearchCondition(json);
        } catch (Exception e) {
            log.error("AI 파싱 실패, fallback 적용", e);
            return fallbackParse(message);
        }
    }

    private SearchCondition fallbackParse(String message) {
        Set<String> stopWords = Set.of(
                "추천", "해줘", "알려줘", "있어", "어디", "뭐", "좀",
                "근처", "주변", "좋은", "맛있는", "해주세요", "궁금"
        );
        String filtered = Arrays.stream(message.split("\\s+"))
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.joining(" "));

        return SearchCondition.builder()
                .keyword(filtered.isBlank() ? message : filtered)
                .build();
    }

    private SearchCondition parseSearchCondition(String jsonContent) {
        try {
            JsonNode node = objectMapper.readTree(jsonContent);

            SearchCondition.SearchConditionBuilder builder = SearchCondition.builder();

            // boardType 파싱
            String boardTypeStr = node.path("boardType").asText(null);
            if (boardTypeStr != null && !boardTypeStr.equals("null")) {
                try {
                    builder.boardType(BoardType.valueOf(boardTypeStr));
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 BoardType: {}", boardTypeStr);
                }
            }

            // address 파싱
            String address = node.path("address").asText(null);
            if (address != null && !address.equals("null")) {
                builder.address(address);
            }

            // hasBadge 파싱
            if (node.path("hasBadge").isBoolean()) {
                builder.hasBadge(node.path("hasBadge").asBoolean());
            }

            // keyword 파싱
            String keyword = node.path("keyword").asText(null);
            if (keyword != null && !keyword.equals("null")) {
                builder.keyword(keyword);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("JSON 파싱 실패", e);
            return SearchCondition.builder().build();
        }
    }

    // ===================== RAG 응답 생성 =====================

    private String generateRAGResponse(String userMessage, List<PostDocument> posts) {
        if (posts.isEmpty()) {
            return "아직 관련 정보가 등록되지 않았어요 😢 " +
                   "다른 키워드로 질문해보시거나, 직접 정보를 등록해주시면 다른 학우들에게도 큰 도움이 돼요!";
        }

        StringBuilder context = new StringBuilder();
        int limit = Math.min(posts.size(), 5);
        for (int i = 0; i < limit; i++) {
            PostDocument doc = posts.get(i);
            context.append(String.format(
                    "[%d] 제목: %s\n내용: %s\n주소: %s\n추천수: %d\n뱃지: %s\n\n",
                    i + 1,
                    doc.getTitle(),
                    truncate(doc.getContent(), 300),
                    doc.getAddress() != null ? doc.getAddress() : "없음",
                    doc.getLikeCount(),
                    Boolean.TRUE.equals(doc.getBadge()) ? "인증됨" : "없음"
            ));
        }

        String prompt = String.format("""
                너는 대학교 캠퍼스 가이드 AI '풀잎'이야.
                아래 커뮤니티 게시글 정보를 바탕으로 학생의 질문에 답변해줘.

                [커뮤니티 게시글]
                %s

                [학생 질문]
                %s

                답변 규칙:
                1. 게시글 정보에 있는 내용만 활용해. 없는 정보는 지어내지 마.
                2. 핵심 정보를 2~3문장으로 요약해서 답변해.
                3. 특히 유용한 게시글이 있으면 제목을 언급해줘.
                4. 배지가 있는 게시글의 정보를 우선적으로 활용해.
                5. 친근한 말투로 답변해.
                """, context, userMessage);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "너는 대학교 캠퍼스 가이드 AI야. 간결하고 친근하게 답변해."),
                Map.of("role", "user", "content", prompt)
        );

        try {
            return openAIClient.chat(messages, 0.7, 300, false);
        } catch (Exception e) {
            log.error("RAG 응답 생성 실패", e);
            return String.format(
                    "관련 게시글 %d개를 찾았어요! 아래에서 확인해보세요 ✨", posts.size());
        }
    }

    private String extractFirstLine(String content) {
        if (content == null || content.isBlank()) return "";
        return content.split("\n")[0];
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
