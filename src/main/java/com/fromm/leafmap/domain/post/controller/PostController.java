package com.fromm.leafmap.domain.post.controller;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostRequestDTO;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.service.PostService;
import com.fromm.leafmap.global.annotation.CurrentMember;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/{boardType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 작성")
    public ApiResponse<PostResponseDTO.AddPostResultDTO> addPost(
            @RequestPart(value = "data") PostRequestDTO.AddPostRequestDTO addPostRequestDTO,
            @PathVariable BoardType boardType,
            @Parameter(description = "이미지 파일")
            @RequestPart(value = "image", required = false) MultipartFile image,
            @CurrentMember Member member) {

        PostResponseDTO.AddPostResultDTO addPostResultDTO = postService.addPost(boardType, addPostRequestDTO, image, member);
        return ApiResponse.onSuccess(addPostResultDTO);
    }
}