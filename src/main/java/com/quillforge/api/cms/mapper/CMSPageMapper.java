package com.quillforge.api.cms.mapper;

import com.quillforge.api.cms.dto.CMSPageRequest;
import com.quillforge.api.cms.dto.CMSPageResponse;
import com.quillforge.api.cms.entity.CMSPage;
import com.quillforge.api.common.mapper.ContentBlockMapper;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {SeoMapper.class, ContentBlockMapper.class})
public interface CMSPageMapper {
    @Mapping(target = "createdBy", source = "createdBy", qualifiedByName = "userToString")
    @Mapping(target = "updatedBy", source = "updatedBy", qualifiedByName = "userToString")
    CMSPageResponse toResponse(CMSPage page);

    CMSPage toEntity(CMSPageRequest request);

    @Named("userToString")
    default String userToString(User user) {
        if (user == null) {
            return null;
        }
        return user.getName() != null ? user.getName() : user.getEmail();
    }
}
