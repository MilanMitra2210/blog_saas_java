package com.quillforge.api.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank
    @JsonProperty("refreshToken")
    @JsonAlias("refresh_token")
    private String refreshToken;
}
