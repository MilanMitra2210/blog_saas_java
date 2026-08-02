package com.quillforge.api.media.service;

import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.media.dto.FolderDto;
import com.quillforge.api.media.entity.Folder;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.mapper.FolderMapper;
import com.quillforge.api.media.repository.FolderRepository;
import com.quillforge.api.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final MediaRepository mediaRepository;
    private final FolderMapper folderMapper;
    private final S3Service s3Service;

    @Override
    public List<FolderDto> getAllFolders() {
        return folderRepository.findAll()
                .stream()
                .map(folderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public FolderDto createFolder(String name, UUID parentId) {
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        Folder saved = folderRepository.save(folder);
        return folderMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteFolder(UUID id) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        
        // Recursive deletion helper
        deleteFolderRecursive(folder);
    }

    private void deleteFolderRecursive(Folder folder) {
        // Delete nested folders first
        List<Folder> subFolders = folderRepository.findByParentId(folder.getId());
        for (Folder sub : subFolders) {
            deleteFolderRecursive(sub);
        }

        // Delete contained media items
        List<Media> mediaList = mediaRepository.findByFolderId(folder.getId());
        for (Media media : mediaList) {
            s3Service.deleteFile(media.getKey());
            media.setDeleted(true);
            media.setDeletedAt(Instant.now());
            mediaRepository.save(media);
        }

        // Soft delete folder
        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
        folderRepository.save(folder);
    }
}
