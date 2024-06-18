package com.example.tgbotcardsonline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Suit {
    HEARTS("HEARTS"),
    DIAMONDS("DIAMONDS"),
    CLUBS("CLUBS"),
    SPADES("SPADES");
    @JsonValue
    private final String suit;

    Suit(String suit){
        this.suit = suit;
    }

    @JsonCreator
    public static Suit forSuit(String suit) {
        for (Suit s : values()) {
            if (s.suit.equalsIgnoreCase(suit)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown card suit: " + suit);
    }

}
