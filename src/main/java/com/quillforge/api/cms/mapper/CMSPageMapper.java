package com.quillforge.api.cms.mapper;

import com.quillforge.api.cms.dto.*;
import com.quillforge.api.cms.entity.*;
import com.quillforge.api.common.mapper.ContentBlockMapper;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.user.mapper.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {SeoMapper.class, ContentBlockMapper.class, UserMapper.class})
public interface CMSPageMapper {
    CMSPageResponse toResponse(CMSPage page);

    CMSPage toEntity(CMSPageRequest request);

    CMSPageMetricDto toDto(CMSPageMetric metric);
    CMSPageMetric toEntity(CMSPageMetricDto dto);
}
