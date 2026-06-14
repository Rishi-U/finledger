package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "User information")
public class UserResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Rishi U")
    private String name;

    @Schema(example = "rishi@gmail.com")
    private String email;
}