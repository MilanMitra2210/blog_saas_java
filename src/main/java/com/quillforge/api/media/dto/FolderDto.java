package com.quillforge.api.media.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FolderDto {
    private UUID id;
    private String name;
    private UUID parentId;
}
