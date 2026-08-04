package com.quillforge.api.enquiry.service;

import com.quillforge.api.common.dto.PaginatedResponse;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.enquiry.dto.EnquiryCreateDto;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;
import com.quillforge.api.enquiry.dto.EnquiryUpdateDto;
import com.quillforge.api.enquiry.entity.Enquiry;
import com.quillforge.api.enquiry.mapper.EnquiryMapper;
import com.quillforge.api.enquiry.repository.EnquiryRepository;
import com.quillforge.api.media.dto.MediaDto;
import com.quillforge.api.media.entity.Folder;
import com.quillforge.api.media.repository.FolderRepository;
import com.quillforge.api.media.repository.MediaRepository;
import com.quillforge.api.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EnquiryMapper enquiryMapper;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public EnquiryResponseDto createEnquiry(EnquiryCreateDto dto, MultipartFile file) {
        Enquiry enquiry = enquiryMapper.toEntity(dto);
        enquiry.setStatus("pending");

        if (dto.getAttachmentId() != null) {
            mediaRepository.findById(dto.getAttachmentId()).ifPresent(enquiry::setAttachment);
        }

        if (file != null && !file.isEmpty()) {
            UUID folderId = getOrCreateEnquiryFolderId();
            try {
                MediaDto mediaDto = mediaService.uploadMedia(file, folderId, "Enquiry Attachment");
                mediaRepository.findById(mediaDto.getId()).ifPresent(enquiry::setAttachment);
            } catch (Exception e) {
                log.error("Failed to upload enquiry attachment", e);
            }
        }

        Enquiry saved = enquiryRepository.save(enquiry);
        return enquiryMapper.toDto(saved);
    }

    @Override
    public PaginatedResponse<EnquiryResponseDto> getAllEnquiries(
            int page,
            int limit,
            String search,
            String status,
            String type
    ) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Enquiry> resultPage = enquiryRepository.findAllFiltered(search, status, type, pageRequest);
        return PaginatedResponse.of(resultPage, enquiryMapper::toDto);
    }

    @Override
    public EnquiryResponseDto getEnquiryById(UUID id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id.toString()));
        return enquiryMapper.toDto(enquiry);
    }

    @Override
    @Transactional
    public EnquiryResponseDto updateEnquiry(UUID id, EnquiryUpdateDto dto) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id.toString()));
        enquiryMapper.updateEntityFromDto(dto, enquiry);
        return enquiryMapper.toDto(enquiry);
    }

    @Override
    @Transactional
    public void deleteEnquiry(UUID id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id.toString()));
        enquiry.setDeleted(true);
        enquiry.setDeletedAt(Instant.now());
        enquiryRepository.save(enquiry);
    }

    private UUID getOrCreateEnquiryFolderId() {
        return folderRepository.findAll().stream()
                .filter(f -> "Enquiries".equalsIgnoreCase(f.getName()))
                .map(Folder::getId)
                .findFirst()
                .orElseGet(() -> {
                    Folder folder = new Folder();
                    folder.setName("Enquiries");
                    return folderRepository.save(folder).getId();
                });
    }
}
