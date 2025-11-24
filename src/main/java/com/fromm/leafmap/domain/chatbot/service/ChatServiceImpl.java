package com.fromm.leafmap.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromm.leafmap.domain.chatbot.dto.ChatRequestDTO;
import com.fromm.leafmap.domain.chatbot.dto.ChatResponseDTO;
import com.fromm.leafmap.domain.chatbot.dto.SearchCondition;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final PostRepository postRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Override
    @Transactional(readOnly = true)
    public ChatResponseDTO.ChatResultDTO processQuery(ChatRequestDTO.ChatQueryDTO request, Member member) {

        // 1. AI로 자연어 파싱
        SearchCondition condition = parseWithAI(request.getMessage());

        log.info("파싱 결과 - boardType: {}, address: {}, hasBadge: {}, keyword: {}",
                condition.getBoardType() != null ? condition.getBoardType().toString() : null,  // ✅ 이미 null 체크 있음
                condition.getAddress(),
                condition.getHasBadge(),
                condition.getKeyword());

        // 2. DB 검색
        List<Post> posts = postRepository.searchPostsByCondition(
                condition.getBoardType() != null ? condition.getBoardType().toString() : null,  // ✅ 이미 null 체크 있음
                condition.getAddress(),
                condition.getHasBadge(),
                condition.getKeyword()
        );

        log.info("검색 결과: {}개", posts.size());

        // 3. DTO 변환
        List<ChatResponseDTO.PostPreviewDTO> postPreviews = posts.stream()
                .map(post -> ChatResponseDTO.PostPreviewDTO.builder()
                        .postId(post.getId())
                        .boardType(post.getBoardType())
                        .title(post.getTitle())
                        .contentPreview(extractFirstLine(post.getContent()))
                        .address(post.getAddress())
                        .imageUrl(post.getImageUrl())
                        .badge(post.getBadge())
                        .likeCount(post.getLikeCount())
                        .createdAt(post.getCreatedAt())
                        .build())
                .toList();

        // 4. 응답 메시지 생성
        String responseMessage = generateResponseMessage(posts.size(), condition);

        return ChatResponseDTO.ChatResultDTO.builder()
                .message(responseMessage)
                .posts(postPreviews)
                .build();
    }

    private SearchCondition parseWithAI(String message) {
        String prompt = String.format("""
        사용자 질문: "%s"
        
        위 질문을 분석해서 검색에 최적화된 JSON을 만들어줘.
        
        규칙:
        - boardType: RESTAURANT(음식/카페), SHORTCUTS(지름길), FACILITY_USAGE(시설), MAJOR_TIPS(전공), CAMPUS_LIFE_TIPS(학교생활)
        - address: 지역명 추출 (예: "성신여대" → "성신")
        - hasBadge: 배지/뱃지 언급 여부
        - keyword: **가장 핵심적인 검색어만 추출** (중요)
          **중요 원칙:**
          1. 핵심 키워드 1-3개 추출
          2. 건물명, 장소명은 반드시 포함
          3. 유사어/동의어 1-2개 포함
          4. 여러 키워드는 띄어쓰기로 구분
           예시)
              - "성신 밥집 추천해줘" → boardType: "RESTAURANT", address: 성신, keyword: "밥집 식당 맛집"
              - "성신여대 근처 카페 추천해줘" → boardType: "RESTAURANT", address: 성신, keyword: "카페 커피"
              - "혜인관까지 지름길" → boardType: "SHORTCUTS", address: null, keyword: "혜인관"
              - "조용한 공부 장소" → boardType: null, address: null, keyword: "조용 공부" 
         
        응답 JSON 형식 (실제 질문 내용에 맞게 값을 채워야 함):
          {"boardType": null, "address": null, "hasBadge": null, "keyword": "실제 추출된 키워드"}    
        """, message);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", List.of(
                            Map.of("role", "system", "content", "너는 검색 쿼리 파서야. JSON만 응답해."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                    "https://api.openai.com/v1/chat/completions",
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return parseSearchCondition(content);

        } catch (Exception e) {
            log.error("AI 파싱 실패, 기본 키워드 검색으로 fallback", e);
            return SearchCondition.builder()
                    .keyword(message)
                    .build();
        }
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

    private String extractFirstLine(String content) {
        if (content == null || content.isBlank()) return "";
        return content.split("\n")[0];
    }

    private String generateResponseMessage(int count, SearchCondition condition) {
        if (count == 0) {
            return "검색 결과가 없어요 😢 다른 키워드로 검색해보시겠어요?";
        }

        StringBuilder message = new StringBuilder();

        if (condition.getAddress() != null) {
            message.append(condition.getAddress()).append(" 근처 ");
        }

        if (condition.getBoardType() != null) {
            message.append(getBoardTypeKorean(condition.getBoardType())).append(" ");
        }

        if (condition.getHasBadge() != null && condition.getHasBadge()) {
            message.append("배지 있는 ");
        }

        message.append(String.format("게시글 %d개를 찾았어요! ✨", count));

        return message.toString();
    }

    private String getBoardTypeKorean(BoardType boardType) {
        return switch (boardType) {
            case RESTAURANT -> "맛집";
            case SHORTCUTS -> "지름길";
            case CAMPUS_LIFE_TIPS -> "학교 생활 꿀팁";
            case FACILITY_USAGE -> "시설 이용 꿀팁";
            case MAJOR_TIPS -> "학과 선택 꿀팁";
        };
    }
}