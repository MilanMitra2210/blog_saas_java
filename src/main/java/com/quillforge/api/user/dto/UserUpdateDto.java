package com.quillforge.api.user.dto;

import com.quillforge.api.user.entity.User.RoleEnum;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserUpdateDto {

    private String name;

    @Email
    private String email;

    private RoleEnum role;
    
    private UUID roleId;

    private Boolean active;

    private Boolean blocked;

    private String blockReason;

    private UUID imageId;
}
