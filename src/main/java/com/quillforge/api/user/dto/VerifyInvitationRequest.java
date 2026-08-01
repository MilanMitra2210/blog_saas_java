package com.quillforge.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyInvitationRequest {

    @NotBlank
    private String token;
}
