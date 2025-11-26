package com.fromm.leafmap.domain.comment.service;

import com.fromm.leafmap.domain.comment.dto.CommentRequestDTO;
import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface CommentService {

    CommentResponseDTO.AddCommentResultDTO addComment(Long postId, CommentRequestDTO.AddCommentDTO request, Member member);

    void deleteComment(Long postId, Long commentId, Member member);
}
