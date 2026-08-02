package com.quillforge.api.media.service;

import com.quillforge.api.media.dto.FolderDto;
import java.util.List;
import java.util.UUID;

public interface FolderService {
    List<FolderDto> getAllFolders();
    FolderDto createFolder(String name, UUID parentId);
    void deleteFolder(UUID id);
}
