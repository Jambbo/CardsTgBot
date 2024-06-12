package com.example.tgbotcardsonline.web.mapper;

import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.web.dto.DeckResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeckResponseMapper extends Mappable<DeckResponse, DeckResponseDto> {
}
