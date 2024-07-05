package com.example.tgbotcardsonline.web.mapper;

import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.enums.Value;
import com.example.tgbotcardsonline.model.response.Card;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CardMapper {

    default Card toCardFromStringCode(String cardCode){
        String codeVal = cardCode.substring(0, cardCode.length() - 1);
        String codeSuit = cardCode.substring(cardCode.length() - 1);

        String suit = switch (codeSuit){
            case "H" ->  "HEARTS";
            case "D" ->  "DIAMONDS";
            case "C" -> "CLUBS";
            case "S" ->  "SPADES";
            default -> throw new IllegalStateException("Unexpected value: " + codeVal);
        };

        String value = switch (codeVal){
            case "6" ->  "6";
            case "7" ->  "7";
            case "8" -> "8";
            case "9" ->  "9";
            case "10", "0" ->  "10";
            case "J" ->  "JACK";
            case "Q" ->  "QUEEN";
            case "K" -> "KING";
            case "A" ->  "ACE";
            default -> throw new IllegalStateException("Unexpected value: " + codeVal);
        };

        return Card.builder()
                .code(cardCode)
                .value(Value.forValue(value))
                .suit(Suit.forSuit(suit))
                .build();
    }

    default List<Card> toCardsFromStringCodes(List<String> cardCodes) {
        return cardCodes.stream()
                .map(this::toCardFromStringCode)
                .collect(Collectors.toList());
    }

}
