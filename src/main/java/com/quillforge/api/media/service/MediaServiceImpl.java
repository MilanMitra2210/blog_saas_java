package com.quillforge.api.media.service;

import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.media.dto.FolderDto;
import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.media.dto.MediaPaginationDto;
import com.quillforge.api.media.dto.MediaResponseDto;
import com.quillforge.api.media.entity.Folder;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.mapper.FolderMapper;
import com.quillforge.api.media.mapper.MediaMapper;
import com.quillforge.api.media.repository.FolderRepository;
import com.quillforge.api.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final FolderRepository folderRepository;
    private final MediaMapper mediaMapper;
    private final FolderMapper folderMapper;
    private final S3Service s3Service;

    @Override
    public MediaResponseDto getMedia(UUID folderId, String search, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Media> mediaPage = mediaRepository.findAllFiltered(folderId, search, pageRequest);

        List<MediaDto> files = mediaPage.getContent()
                .stream()
                .map(mediaMapper::toDto)
                .toList();

        // Subfolders: Fetch immediately nested folders
        List<Folder> folders = folderRepository.findByParentId(folderId);
        List<FolderDto> folderDtos = folders.stream()
                .map(folderMapper::toDto)
                .toList();

        MediaPaginationDto pagination = MediaPaginationDto.builder()
                .total(mediaPage.getTotalElements())
                .page(page)
                .limit(limit)
                .totalPages(mediaPage.getTotalPages())
                .build();

        return MediaResponseDto.builder()
                .files(files)
                .folders(folderDtos)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public MediaDto uploadMedia(MultipartFile file, UUID folderId, String altText) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "upload_" + UUID.randomUUID().toString();
            }

            // Generate unique S3 Key
            String s3Key = "media/" + UUID.randomUUID().toString() + "_" + originalFilename;

            // Upload
            String fileUrl = s3Service.uploadFile(file, s3Key);

            Media media = new Media();
            media.setName(originalFilename);
            media.setAltText(altText);
            media.setKey(s3Key);
            media.setUrl(fileUrl);
            media.setSize(file.getSize());
            media.setMimeType(file.getContentType());
            media.setFolderId(folderId);

            // Attempt to resolve image dimensions
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                try {
                    BufferedImage img = ImageIO.read(file.getInputStream());
                    if (img != null) {
                        media.setWidth(img.getWidth());
                        media.setHeight(img.getHeight());
                    }
                } catch (Exception e) {
                    log.warn("Could not parse image dimensions for {}", originalFilename);
                }
            }

            Media saved = mediaRepository.save(media);
            return mediaMapper.toDto(saved);
        } catch (IOException e) {
            log.error("Failed to upload media file", e);
            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<MediaDto> uploadMediaBulk(MultipartFile[] files, UUID folderId) {
        List<MediaDto> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                uploaded.add(uploadMedia(file, folderId, null));
            }
        }
        return uploaded;
    }

    @Override
    @Transactional
    public MediaDto replaceMedia(UUID id, MultipartFile file) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", id));

        try {
            // Delete old file
            s3Service.deleteFile(media.getKey());

            // Upload new file on same key (or generate new one to avoid cache)
            String originalFilename = file.getOriginalFilename();
            String s3Key = "media/" + UUID.randomUUID().toString() + "_" + originalFilename;
            String fileUrl = s3Service.uploadFile(file, s3Key);

            media.setName(originalFilename);
            media.setKey(s3Key);
            media.setUrl(fileUrl);
            media.setSize(file.getSize());
            media.setMimeType(file.getContentType());

            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                try {
                    BufferedImage img = ImageIO.read(file.getInputStream());
                    if (img != null) {
                        media.setWidth(img.getWidth());
                        media.setHeight(img.getHeight());
                    }
                } catch (Exception e) {
                    log.warn("Could not parse image dimensions for {}", originalFilename);
                }
            }

            Media saved = mediaRepository.save(media);
            return mediaMapper.toDto(saved);
        } catch (IOException e) {
            log.error("Failed to replace media file", e);
            throw new BadRequestException("File replacement failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MediaDto updateMedia(UUID id, String name, String altText, UUID folderId) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", id));

        if (name != null) media.setName(name);
        if (altText != null) media.setAltText(altText);
        if (folderId != null) media.setFolderId(folderId);

        Media saved = mediaRepository.save(media);
        return mediaMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void bulkMoveMedia(List<UUID> mediaIds, UUID folderId) {
        for (UUID mediaId : mediaIds) {
            mediaRepository.findById(mediaId).ifPresent(media -> {
                media.setFolderId(folderId);
                mediaRepository.save(media);
            });
        }
    }

    @Override
    @Transactional
    public void deleteMedia(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", id));

        // Delete from S3
        s3Service.deleteFile(media.getKey());

        // Soft delete from DB
        media.setDeleted(true);
        media.setDeletedAt(Instant.now());
        mediaRepository.save(media);
    }
}
