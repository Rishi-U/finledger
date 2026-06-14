package com.rishi.finledger.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.rishi.finledger.entity.WalletEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletEntity w WHERE w.user.id = :userId")
    Optional<WalletEntity> findByUserIdForUpdate(Long userId);

    Optional<WalletEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletEntity w WHERE w.user.email = :email")
    Optional<WalletEntity> findByUserEmailForUpdate(@Param("email") String email);

    Optional<WalletEntity> findByUserEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    Optional<WalletEntity> findByIdForUpdate(Long id);
}
