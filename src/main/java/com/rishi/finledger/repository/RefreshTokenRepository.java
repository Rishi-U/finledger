package com.rishi.finledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishi.finledger.entity.RefreshTokenEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    void deleteByUserId(Long userId);
}