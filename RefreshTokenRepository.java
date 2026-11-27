package com.fooddelivery.repository;

import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked=true WHERE r.user=:user")
    void revokeAllByUser(User user);
}
