package com.quillforge.api.blog.mapper;

import com.quillforge.api.blog.dto.*;
import com.quillforge.api.blog.entity.*;
import com.quillforge.api.common.mapper.SeoMapper;
import com.quillforge.api.media.mapper.MediaMapper;
import com.quillforge.api.user.mapper.UserMapper;
import com.quillforge.api.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {SeoMapper.class, MediaMapper.class, UserMapper.class})
public interface BlogMapper {

    BlogCategoryDto toDto(BlogCategory category);
    BlogCategory toEntity(BlogCategoryDto dto);

    BlogAuthorDto toDto(BlogAuthor author);
    BlogAuthor toEntity(BlogAuthorDto dto);

    TagDto toDto(Tag tag);
    Tag toEntity(TagDto dto);

    BlogMetricDto toDto(BlogMetric metric);
    BlogMetric toEntity(BlogMetricDto dto);

    BlogRevisionDto toDto(BlogRevision revision);

    BlogSectionDto toDto(BlogSection section);
    BlogSection toEntity(BlogSectionDto dto);

    BlogSubSectionDto toDto(BlogSubSection subSection);
    BlogSubSection toEntity(BlogSubSectionDto dto);

    BlogResponse toResponse(Blog blog);
}
