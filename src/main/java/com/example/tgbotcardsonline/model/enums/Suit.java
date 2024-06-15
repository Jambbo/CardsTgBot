package com.example.tgbotcardsonline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Suit {
    HEARTS("H"),
    DIAMONDS("D"),
    CLUBS("C"),
    SPADES("S");
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
