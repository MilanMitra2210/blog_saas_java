package com.quillforge.api.user.dto;

import com.quillforge.api.user.entity.User.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateUserDto {

    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private RoleEnum role = RoleEnum.USER;

    private UUID roleId;

    private boolean active = true;

    private UUID imageId;
}
