package com.fromm.leafmap.domain.post.controller;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostLikeResponseDTO;
import com.fromm.leafmap.domain.post.dto.PostRequestDTO;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.service.MajortipsPostInitService;
import com.fromm.leafmap.domain.post.service.PostLikeService;
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
@RequestMapping("/api/boards")
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final MajortipsPostInitService majortipsPostInitService;

    @PostMapping(value = "/MAJOR_TIPS/init")
    @Operation(summary = "학과 게시판 게시글 생성 (연동 X)")
    public ApiResponse<String> createMajortipsPosts() {
        majortipsPostInitService.createPostsFromMajors();
        return ApiResponse.onSuccess("Majortips 게시글 생성 완료");
    }

    @PostMapping(value = "/{boardType}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 작성", description = "게시글 정보는 JSON 형식으로, 이미지는 Multipart(Form-Data) 형식으로 함께 전달해주세요.\n\n" )
    public ApiResponse<PostResponseDTO.AddPostResultDTO> addPost(
            @RequestPart(value = "data") PostRequestDTO.AddPostRequestDTO addPostRequestDTO,
            @PathVariable BoardType boardType,
            @Parameter(description = "이미지 파일")
            @RequestPart(value = "image", required = false) MultipartFile image,
            @CurrentMember Member member) {

        PostResponseDTO.AddPostResultDTO addPostResultDTO = postService.addPost(boardType, addPostRequestDTO, image, member);
        return ApiResponse.onSuccess(addPostResultDTO);
    }

    @GetMapping(value = "/{boardType}/posts")
    @Operation(summary = "게시판 목록 조회", description = "첫 페이지 조회 시 cursor 값으로 0을 전달해주세요.\n\n" +
            "첫 페이지가 아닌 경우 이전 응답의 hasNext가 true일 때, nextCursor 값을 cursor로 전달해주세요.")
    public ApiResponse<PostResponseDTO.PostListResultDTO> getPostList(
            @PathVariable BoardType boardType,
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @CurrentMember Member member) {

        PostResponseDTO.PostListResultDTO postListResultDTO = postService.getPostList(boardType, member, cursor, limit);
        return ApiResponse.onSuccess(postListResultDTO);
    }

    @GetMapping(value = "/{boardType}/posts/{postId}")
    @Operation(summary = "게시판 상세 조회")
    public ApiResponse<PostResponseDTO.PostDetailResultDTO> getPostDetail(
            @PathVariable BoardType boardType,
            @PathVariable Long postId,
            @CurrentMember Member member) {

        PostResponseDTO.PostDetailResultDTO postDetailResultDTO = postService.getPostDetail(boardType, postId, member);
        return ApiResponse.onSuccess(postDetailResultDTO);
    }

    @PostMapping(value ="/{boardType}/posts/{postId}/like")
    @Operation(summary = "게시글 추천 토글", description = "추천하지 않은 상태라면 추천이 추가되고, 이미 추천한 상태라면 추천이 취소됩니다.")
    public ApiResponse<PostLikeResponseDTO.PostLikeResultDTO> toggleLike(
            @PathVariable BoardType boardType,
            @PathVariable Long postId,
            @CurrentMember Member member) {

        PostLikeResponseDTO.PostLikeResultDTO postLikeResultDTO = postLikeService.toggleLike(boardType, postId, member);
        return ApiResponse.onSuccess(postLikeResultDTO);
    }
}