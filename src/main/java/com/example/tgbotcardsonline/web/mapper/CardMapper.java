package com.example.tgbotcardsonline.web.mapper;

import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.enums.Value;
import com.example.tgbotcardsonline.model.response.Card;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardMapper {

    default Card toCardFromStringCode(String cardCode){

        return Card.builder()
                .code(cardCode)
                .value(Value.forValue(cardCode.substring(0, cardCode.length() - 1)))
                .suit(Suit.forSuit(cardCode.substring(cardCode.length() - 1)))
                .build();
    }

}
