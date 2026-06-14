package com.rishi.finledger.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishi.finledger.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByEmail (String email);
}
