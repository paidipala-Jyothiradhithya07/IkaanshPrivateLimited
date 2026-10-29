package com.ikaanshpl.finance.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ikaanshpl.finance.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Row lock: two concurrent refreshes with the same token are forced to run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.familyId = :familyId and r.revoked = false")
    int revokeFamily(@Param("familyId") String familyId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);

    boolean existsByFamilyIdAndRevokedFalseAndExpiresAtAfter(String familyId, Instant now);
}
