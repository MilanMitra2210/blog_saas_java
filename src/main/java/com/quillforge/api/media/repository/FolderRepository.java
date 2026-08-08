package com.quillforge.api.media.repository;

import com.quillforge.api.media.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByParentId(UUID parentId);
}
