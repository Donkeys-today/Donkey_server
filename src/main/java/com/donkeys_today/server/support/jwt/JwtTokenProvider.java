package com.donkeys_today.server.support.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}")
    private String JWT_SECRET;
    public String issueAccessToken(Long userId, String role) {
        return generateToken(JWTConstants.ACCESS_TOKEN, userId, role, JWTConstants.ACCESS_TOKEN_EXPIRATION_TIME);
    }

    public String issueRefreshToken(Long userId, String role) {
        String refreshToken = generateToken(JWTConstants.REFRESH_TOKEN, userId, role, JWTConstants.REFRESH_TOKEN_EXPIRATION_TIME);
        redisTemplate.opsForValue().set(
                String.valueOf(userId),
                refreshToken,
                JWTConstants.REFRESH_TOKEN_EXPIRATION_TIME,
                TimeUnit.MILLISECONDS
        );
        return refreshToken;
    }

    public String generateToken(String type, Long userId, String role, Long tokenExpirationTime) {
        final Date now = new Date();
        final Claims claims = Jwts.claims()
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenExpirationTime));      // 만료 시간

        claims.put(JWTConstants.TOKEN_TYPE, type);
        claims.put(JWTConstants.USER_ID, String.valueOf(userId));
        claims.put(JWTConstants.ROLE, role);

        String token = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // Header
                .setClaims(claims) // Claim
                .signWith(getSigningKey()) // Signature
                .compact();
        return token;
    }

    public SecretKey getSigningKey() {
        String encodedKey = Base64.getEncoder().encodeToString(JWT_SECRET.getBytes()); //SecretKey 통해 서명 생성
        return Keys.hmacShaKeyFor(
                encodedKey.getBytes());
    }
}