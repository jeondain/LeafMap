package com.fromm.leafmap.domain.chatbot.controller;

import com.fromm.leafmap.domain.chatbot.dto.ChatRequestDTO;
import com.fromm.leafmap.domain.chatbot.dto.ChatResponseDTO;
import com.fromm.leafmap.domain.chatbot.service.ChatService;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.global.annotation.CurrentMember;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "챗봇 게시글 검색")
    public ApiResponse<ChatResponseDTO.ChatResultDTO> chat(
            @RequestBody ChatRequestDTO.ChatQueryDTO request,
            @CurrentMember Member member) {

        ChatResponseDTO.ChatResultDTO result = chatService.processQuery(request, member);
        return ApiResponse.onSuccess(result);
    }
}
