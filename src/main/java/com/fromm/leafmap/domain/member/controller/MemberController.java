package com.fromm.leafmap.domain.member.controller;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.service.MemberService;
import com.fromm.leafmap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ApiResponse<String> signUp(@RequestBody @Valid MemberRequestDTO.MemberSignupDTO memberSignupDTO) throws Exception {
        memberService.signup(memberSignupDTO);
        return ApiResponse.onSuccess("회원가입 성공");
    }
}
