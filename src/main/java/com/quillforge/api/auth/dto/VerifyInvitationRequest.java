package com.quillforge.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyInvitationRequest {

    @NotBlank
    private String token;
}
