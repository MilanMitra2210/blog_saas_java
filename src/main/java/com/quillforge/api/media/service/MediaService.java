package com.quillforge.api.media.service;

import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.media.dto.MediaResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {
    MediaResponseDto getMedia(UUID folderId, String search, int page, int limit);
    MediaDto uploadMedia(MultipartFile file, UUID folderId, String altText);
    List<MediaDto> uploadMediaBulk(MultipartFile[] files, UUID folderId);
    MediaDto replaceMedia(UUID id, MultipartFile file);
    MediaDto updateMedia(UUID id, String name, String altText, UUID folderId);
    void bulkMoveMedia(List<UUID> mediaIds, UUID folderId);
    void deleteMedia(UUID id);
}
