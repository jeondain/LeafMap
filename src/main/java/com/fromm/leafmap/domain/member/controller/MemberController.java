package com.fromm.leafmap.domain.member.controller;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.member.service.MemberService;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.global.annotation.CurrentMember;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ApiResponse<String> signUp(@RequestBody @Valid MemberRequestDTO.MemberSignupDTO memberSignupDTO) {
        memberService.signup(memberSignupDTO);
        return ApiResponse.onSuccess("회원가입 성공");
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ApiResponse<MemberResponseDTO.MemberLoginResultDTO> login(@RequestBody @Valid MemberRequestDTO.MemberLoginDTO memberLoginDTO) {
        MemberResponseDTO.MemberLoginResultDTO memberLoginResultDTO = memberService.login(memberLoginDTO);
        return ApiResponse.onSuccess(memberLoginResultDTO);
    }

    @GetMapping("/member")
    @Operation(summary = "회원 정보 조회")
    public ApiResponse<MemberResponseDTO.GetMemberInfoResultDTO> getMemberInfo(
            @CurrentMember Member member) {

        MemberResponseDTO.GetMemberInfoResultDTO getMemberInfoResultDTO = memberService.getMemberInfo(member);
        return ApiResponse.onSuccess(getMemberInfoResultDTO);
    }

    @PatchMapping("/member")
    @Operation(summary = "회원 정보 수정")
    public ApiResponse<MemberResponseDTO.GetMemberInfoResultDTO> updateMemberInfo(
            @RequestBody MemberRequestDTO.UpdateMemberInfoDTO updateMemberInfoDTO,
            @CurrentMember Member member) {

        MemberResponseDTO.GetMemberInfoResultDTO getMemberInfoResultDTO = memberService.updateMemberInfo(updateMemberInfoDTO, member);
        return ApiResponse.onSuccess(getMemberInfoResultDTO);
    }

    @GetMapping("/member/my-posts")
    @Operation(summary = "내가 작성한 글 조회", description = "운영자 승인을 받은 게시글은 isPublic 값이 true로 표시됩니다.")
    public ApiResponse<PostResponseDTO.PostListResultDTO> getMemberPosts(
            @CurrentMember Member member,
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        PostResponseDTO.PostListResultDTO postListResultDTO = memberService.getMemberPosts(member, cursor, limit);
        return ApiResponse.onSuccess(postListResultDTO);
    }

    @GetMapping("/member/liked-posts")
    @Operation(summary = "내가 추천한 글 조회")
    public ApiResponse<PostResponseDTO.PostListResultDTO> getPostsLikedByMember(
            @CurrentMember Member member,
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        PostResponseDTO.PostListResultDTO postListResultDTO = memberService.getPostsLikedByMember(member, cursor, limit);
        return ApiResponse.onSuccess(postListResultDTO);
    }

    @GetMapping("/member/my-comments")
    @Operation(summary = "내가 댓글 단 게시글 조회")
    public ApiResponse<PostResponseDTO.PostListResultDTO> getPostsCommentedByMember(
            @CurrentMember Member member,
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        PostResponseDTO.PostListResultDTO postListResultDTO = memberService.getPostsCommentedByMember(member, cursor, limit);
        return ApiResponse.onSuccess(postListResultDTO);
    }
}
