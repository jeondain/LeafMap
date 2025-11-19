package com.fromm.leafmap.domain.comment.service;

import com.fromm.leafmap.domain.comment.dto.CommentRequestDTO;
import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface CommentService {
    public CommentResponseDTO.AddCommentResultDTO addComment(Long postId, CommentRequestDTO.AddCommentDTO request, Member member);
}
