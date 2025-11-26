package com.fromm.leafmap.global.security;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> NO_CHECK_URLS = Set.of(
            "/api/login",
            "/api/logout"
    );

    private final JwtService jwtService;
    private final MemberRepository memberRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (NO_CHECK_URLS.contains(request.getRequestURI())) {
            log.info("인증 체크 제외 URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        checkAccessTokenAndAuthentication(request, response, filterChain);
    }

    public void checkAccessTokenAndAuthentication(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        log.info("checkAccessTokenAndAuthentication() 호출");

        try {
            jwtService.extractAccessToken(request)
                    .filter(jwtService::isTokenValid)  // 유효성 검증
                    .flatMap(jwtService::extractLoginId)  // loginId 추출
                    .flatMap(memberRepository::findByLoginId)  // 사용자 조회
                    .ifPresentOrElse(
                            this::saveAuthentication,  // 인증 처리
                            () -> log.warn("인증 실패: 유효하지 않은 토큰이거나 사용자를 찾을 수 없습니다.")
                    );

        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    public void saveAuthentication(Member member) {
        UserDetails userDetailsUser = org.springframework.security.core.userdetails.User.builder()
                .username(member.getLoginId())
                .password(member.getPassword())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetailsUser,
                null,
                authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("인증 성공: {}", member.getLoginId());
    }
}