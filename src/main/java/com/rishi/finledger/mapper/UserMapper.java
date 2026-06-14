package com.rishi.finledger.mapper;

import org.springframework.stereotype.Component;

import com.rishi.finledger.dto.RegisterRequest;
import com.rishi.finledger.dto.UserResponse;
import com.rishi.finledger.entity.UserEntity;

@Component
public class UserMapper {

    public UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail());
    }

    public UserEntity toEntity(RegisterRequest request) {
        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        return user;
    }
}
