package com.rishi.finledger.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.rishi.finledger.entity.RefreshTokenEntity;
import com.rishi.finledger.entity.UserEntity;

@Component
public class RefreshTokenMapper {

    public RefreshTokenEntity toEntity(String refreshToken, UserEntity user) {

        RefreshTokenEntity entity = new RefreshTokenEntity();

        entity.setToken(refreshToken);
        entity.setUserId(user.getId());
        entity.setExpiryDate(LocalDateTime.now().plusDays(7));
        entity.setRevoked(false);

        return entity;
    }
}