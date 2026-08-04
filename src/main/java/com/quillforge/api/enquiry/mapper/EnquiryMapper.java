package com.quillforge.api.enquiry.mapper;

import com.quillforge.api.enquiry.dto.EnquiryCreateDto;
import com.quillforge.api.enquiry.dto.EnquiryResponseDto;
import com.quillforge.api.enquiry.dto.EnquiryUpdateDto;
import com.quillforge.api.enquiry.entity.Enquiry;
import com.quillforge.api.media.mapper.MediaMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface EnquiryMapper {

    EnquiryResponseDto toDto(Enquiry enquiry);

    @Mapping(target = "attachment", ignore = true)
    Enquiry toEntity(EnquiryCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(EnquiryUpdateDto dto, @MappingTarget Enquiry enquiry);
}
