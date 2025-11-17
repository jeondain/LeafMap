package com.fromm.leafmap.global.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secretKeyString) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
        this.secretKey = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    // Access Token 생성
    public String generateAccessToken(String loginId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Access Token에서 loginId 추출
    public Optional<String> extractLoginId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return Optional.ofNullable(claims.getSubject());
        } catch (Exception e) {
            log.error("토큰에서 loginId 추출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // 토큰 유효성 검증
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("만료된 토큰입니다.");
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 토큰입니다.");
            return false;
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 토큰입니다.");
            return false;
        } catch (SignatureException e) {
            log.error("서명 검증에 실패했습니다.");
            return false;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 토큰입니다.");
            return false;
        }
    }
}