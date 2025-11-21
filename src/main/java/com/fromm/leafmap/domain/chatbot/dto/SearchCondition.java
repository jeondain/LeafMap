package com.fromm.leafmap.domain.chatbot.dto;

import com.fromm.leafmap.domain.post.entity.BoardType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCondition {
    private BoardType boardType;
    private String address;
    private Boolean hasBadge;
    private String keyword;
}