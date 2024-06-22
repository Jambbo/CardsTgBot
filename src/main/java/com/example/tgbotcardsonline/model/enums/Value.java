package com.example.tgbotcardsonline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Value {

    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("JACK", 11),
    QUEEN("QUEEN", 12),
    KING("KING", 13),
    ACE("ACE", 14);

    @JsonValue
    private final String value;
    private final int rank;

    Value(String value,int rank) {
        this.value = value;
        this.rank = rank;
    }

    @JsonCreator
    public static Value forValue(String value) {
        for (Value v : values()) {
            if (v.value.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown card value: " + value);
    }
    public boolean isHigherThan(Value other) {
        return this.rank > other.rank;
    }

}
