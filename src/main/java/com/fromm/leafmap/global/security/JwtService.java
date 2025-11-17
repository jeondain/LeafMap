package com.fromm.leafmap.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ACCESS_TOKEN_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(ACCESS_TOKEN_HEADER))
                .filter(token -> token.startsWith(BEARER_PREFIX))
                .map(token -> token.substring(BEARER_PREFIX.length()));
    }

    // Access Token을 응답 헤더에 설정
    public void setAccessTokenHeader(HttpServletResponse response, String accessToken) {
        response.setHeader(ACCESS_TOKEN_HEADER, BEARER_PREFIX + accessToken);
    }

    // 토큰 유효성 검증
    public boolean isTokenValid(String token) {
        return jwtProvider.isTokenValid(token);
    }

    // 토큰에서 loginId 추출
    public Optional<String> extractLoginId(String token) {
        return jwtProvider.extractLoginId(token);
    }

    // Access Token 생성
    public String createAccessToken(String loginId) {
        return jwtProvider.generateAccessToken(loginId);
    }
}