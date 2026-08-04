package com.quillforge.api.cms.service;

import com.quillforge.api.cms.dto.CMSPageRequest;
import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.entity.CMSPage;
import com.quillforge.api.cms.mapper.CMSPageMapper;
import com.quillforge.api.cms.repository.CMSPageRepository;
import com.quillforge.api.common.dto.ContentBlockDto;
import com.quillforge.api.common.entity.ContentBlock;
import com.quillforge.api.common.entity.SeoMetadata;
import com.quillforge.api.common.exception.BadRequestException;
import com.quillforge.api.common.exception.ResourceNotFoundException;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.media.entity.Media;
import com.quillforge.api.media.mapper.MediaMapper;
import com.quillforge.api.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;
import java.time.Instant;
import java.util.UUID;

import com.quillforge.api.common.service.RevalidationService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CMSPageServiceImpl implements CMSPageService {

    private final RevalidationService revalidationService;

    private final CMSPageRepository cmsPageRepository;
    private final MediaRepository mediaRepository;
    private final CMSPageMapper cmsPageMapper;
    private final MediaMapper mediaMapper;
    private final SeoMapper seoMapper;

    @Override
    @Cacheable(value = "cms_pages_list", key = "'list:' + (#search ?: '') + '-' + (#status ?: '') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<CMSPageResponse> getCMSPages(String search, String status, Pageable pageable) {
        Boolean isActive = null;
        if ("active".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("inactive".equalsIgnoreCase(status)) {
            isActive = false;
        }

        Page<CMSPage> cmsPage = cmsPageRepository.findAllFiltered(search, isActive, pageable);

        return cmsPage.map(p -> {
            CMSPageResponse dto = cmsPageMapper.toResponse(p);
            // Enrich with metaImage details if imageId exists
            if (p.getSeo() != null && p.getSeo().getMetaImageId() != null) {
                Media media = mediaRepository.findById(p.getSeo().getMetaImageId()).orElse(null);
                if (media != null && dto.getSeo() != null) {
                    dto.getSeo().setMetaImage(mediaMapper.toDto(media));
                }
            }
            return dto;
        });
    }

    @Override
    @Transactional
    public CMSPageResponse createCMSPage(CMSPageRequest request) {
        if (cmsPageRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("CMS Page with slug " + request.getSlug() + " already exists");
        }

        CMSPage page = cmsPageMapper.toEntity(request);
        
        // Associate nested ContentBlocks with page ID
        if (page.getContentBlocks() != null) {
            for (ContentBlock block : page.getContentBlocks()) {
                block.setPageId(page.getId());
            }
        }

        CMSPage saved = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", saved.getSlug(), "create");
        return cmsPageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CMSPageResponse updateCMSPage(UUID id, CMSPageRequest request) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));

        if (!page.getSlug().equals(request.getSlug()) && cmsPageRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("CMS Page with slug " + request.getSlug() + " already exists");
        }

        page.setName(request.getName());
        page.setSlug(request.getSlug());
        page.setActive(request.isActive());

        // Update SEO Metadata
        if (request.getSeo() != null) {
            if (page.getSeo() == null) {
                page.setSeo(seoMapper.toEntity(request.getSeo()));
            } else {
                SeoMetadata seo = page.getSeo();
                seo.setMetaTitle(request.getSeo().getMetaTitle());
                seo.setMetaDescription(request.getSeo().getMetaDescription());
                seo.setMetaKeywords(request.getSeo().getMetaKeywords());
                seo.setMetaRobots(request.getSeo().getMetaRobots());
                seo.setMetaImageId(request.getSeo().getMetaImageId());
                seo.setJsonLd(request.getSeo().getJsonLd());
            }
        } else {
            page.setSeo(null);
        }

        // Update Content Blocks
        page.getContentBlocks().clear();
        if (request.getContentBlocks() != null) {
            for (ContentBlockDto dto : request.getContentBlocks()) {
                ContentBlock block = new ContentBlock();
                block.setBlockKey(dto.getBlockKey());
                block.setContent(dto.getContent());
                block.setPageId(page.getId());
                page.getContentBlocks().add(block);
            }
        }

        CMSPage updated = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", updated.getSlug(), "update");
        return cmsPageMapper.toResponse(updated);
    }

    @Override
    public CMSPageResponse getCMSPageById(UUID id) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));
        
        CMSPageResponse dto = cmsPageMapper.toResponse(page);
        if (page.getSeo() != null && page.getSeo().getMetaImageId() != null) {
            Media media = mediaRepository.findById(page.getSeo().getMetaImageId()).orElse(null);
            if (media != null && dto.getSeo() != null) {
                dto.getSeo().setMetaImage(mediaMapper.toDto(media));
            }
        }
        return dto;
    }

    @Override
    @Cacheable(value = "cms_pages", key = "#slug")
    public CMSPageResponse getCMSPageBySlug(String slug) {
        CMSPage page = cmsPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage with slug: " + slug));
        
        CMSPageResponse dto = cmsPageMapper.toResponse(page);
        if (page.getSeo() != null && page.getSeo().getMetaImageId() != null) {
            Media media = mediaRepository.findById(page.getSeo().getMetaImageId()).orElse(null);
            if (media != null && dto.getSeo() != null) {
                dto.getSeo().setMetaImage(mediaMapper.toDto(media));
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public void deleteCMSPage(UUID id) {
        CMSPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CMSPage", id));

        page.setDeleted(true);
        page.setDeletedAt(Instant.now());
        CMSPage saved = cmsPageRepository.save(page);
        revalidationService.revalidate("cms_page", saved.getSlug(), "delete");
    }
}
