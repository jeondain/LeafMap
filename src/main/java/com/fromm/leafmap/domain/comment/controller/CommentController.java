package com.fromm.leafmap.domain.comment.controller;

import com.fromm.leafmap.domain.comment.dto.CommentRequestDTO;
import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.comment.service.CommentService;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.global.annotation.CurrentMember;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "댓글 작성")
    public ApiResponse<CommentResponseDTO.AddCommentResultDTO> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDTO.AddCommentDTO addCommentDTO,
            @CurrentMember Member member) {

        CommentResponseDTO.AddCommentResultDTO addCommentResultDTO = commentService.addComment(postId, addCommentDTO, member);
        return ApiResponse.onSuccess(addCommentResultDTO);
    }
}

