package com.ikaanshpl.finance.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.entity.RefreshToken;
import com.ikaanshpl.finance.repo.RefreshTokenRepository;


@Service
public class RefreshTokenService {
	 private static final SecureRandom RANDOM = new SecureRandom();

	    private final RefreshTokenRepository repo;
	    private final Duration ttl;

	    public RefreshTokenService(RefreshTokenRepository repo, @Value("${app.jwt.refresh-ttl-days}") long ttlDays) {
	        this.repo = repo;
	        this.ttl = Duration.ofDays(ttlDays);
	    }

	    public record Session(Long userId, String familyId, String rawToken) {}

	    /** New login = new family. */
	    @Transactional
	    public Session startSession(Long userId) {
	        return issue(userId, UUID.randomUUID().toString());
	    }

	    /**
	     * noRollbackFor is essential: when we detect reuse we revoke the family AND throw 401.
	     * Without it the exception would roll the revocation back and the attack would go unpunished.
	     */
	    @Transactional(noRollbackFor = ApiException.class)
	    public Session rotate(String rawToken) {
	        RefreshToken current = repo.findByTokenHashForUpdate(hash(rawToken))
	                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

	        if (current.isRevoked()) {
	            repo.revokeFamily(current.getFamilyId());
	            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected - session revoked");
	        }
	        if (current.getExpiresAt().isBefore(Instant.now())) {
	            repo.revokeFamily(current.getFamilyId());
	            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
	        }
	        current.setRevoked(true);
	        return issue(current.getUserId(), current.getFamilyId());
	    }

	    /** Logout of one session. Unknown tokens are ignored (idempotent). */
	    @Transactional
	    public void revokeSession(String rawToken) {
	        repo.findByTokenHash(hash(rawToken)).ifPresent(t -> repo.revokeFamily(t.getFamilyId()));
	    }

	    /** Logout everywhere. */
	    @Transactional
	    public void revokeAll(Long userId) {
	        repo.revokeAllForUser(userId);
	    }

	    private Session issue(Long userId, String familyId) {
	        byte[] bytes = new byte[32];
	        RANDOM.nextBytes(bytes);
	        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

	        RefreshToken rt = new RefreshToken();
	        rt.setUserId(userId);
	        rt.setFamilyId(familyId);
	        rt.setTokenHash(hash(raw));
	        rt.setExpiresAt(Instant.now().plus(ttl));
	        repo.save(rt);
	        return new Session(userId, familyId, raw);
	    }

	    static String hash(String raw) {
	        try {
	            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
	            return HexFormat.of().formatHex(d);
	        } catch (NoSuchAlgorithmException e) {
	            throw new IllegalStateException(e);
	        }
	    }
}
