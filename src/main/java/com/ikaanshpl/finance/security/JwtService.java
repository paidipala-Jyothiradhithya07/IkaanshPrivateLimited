package com.ikaanshpl.finance.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;

import com.ikaanshpl.finance.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtService {
	 private final SecretKey key;
	    private final long accessTtlSeconds;

	    public JwtService(@Value("${app.jwt.secret}") String secret,
	                      @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds) {
	        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
	        if (bytes.length < 32) throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
	        this.key = Keys.hmacShaKeyFor(bytes);
	        this.accessTtlSeconds = accessTtlSeconds;
	    }

	    /** @param sessionId the refresh-token family; lets us kill the access token when that session is revoked. */
	    public String generateAccessToken(User user, String sessionId) {
	        Instant now = Instant.now();
	        return Jwts.builder()
	                .subject(String.valueOf(user.getId()))
	                .claim("email", user.getEmail())
	                .claim("sid", sessionId)
	                .issuedAt(Date.from(now))
	                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
	                .signWith(key)
	                .compact();
	    }

	    /** @throws JwtException if the signature is wrong, the token is malformed or expired. */
	    public Claims parse(String token) {
	        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	    }

	    public long getAccessTtlSeconds() { 
	    	return accessTtlSeconds;
	    	}
	    
}
