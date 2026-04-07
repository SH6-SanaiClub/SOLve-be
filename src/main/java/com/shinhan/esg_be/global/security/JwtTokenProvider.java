package com.shinhan.esg_be.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String loginId) {
        return createToken(loginId, ACCESS_TOKEN_TYPE, accessTokenValidityInMilliseconds);
    }

    public String createRefreshToken(String loginId) {
        return createToken(loginId, REFRESH_TOKEN_TYPE, refreshTokenValidityInMilliseconds);
    }

    public String getLoginId(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }

    public long getRefreshTokenValidityInMilliseconds() {
        return refreshTokenValidityInMilliseconds;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(String loginId, String tokenType, long validityInMilliseconds) {
        Claims claims = Jwts.claims().setSubject(loginId);
        claims.put(TOKEN_TYPE_CLAIM, tokenType);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String getTokenType(String token) {
        return getClaims(token)
                .get(TOKEN_TYPE_CLAIM, String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
