package com.fromm.leafmap.domain.chatbot.service;

import com.fromm.leafmap.domain.chatbot.dto.ChatRequestDTO;
import com.fromm.leafmap.domain.chatbot.dto.ChatResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface ChatService {
    ChatResponseDTO.ChatResultDTO processQuery(ChatRequestDTO.ChatQueryDTO request, Member member);
}
