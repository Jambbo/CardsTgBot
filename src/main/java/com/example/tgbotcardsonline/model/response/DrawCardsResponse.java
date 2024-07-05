package com.example.tgbotcardsonline.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DrawCardsResponse {
    private Long id;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("deck_id")
    private String deckId;

    @JsonProperty("cards")
    private List<Card> cards;

    @JsonProperty("remaining")
    private int remaining;
}
