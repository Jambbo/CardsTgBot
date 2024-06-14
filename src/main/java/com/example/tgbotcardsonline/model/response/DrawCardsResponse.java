package com.example.tgbotcardsonline.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
//@Entity
public class DrawCardsResponse {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("deck_id")
    private String deckId;
//    @OneToMany
    @JsonProperty("cards")
    private List<Card> cards;
    @JsonProperty("remaining")
    private int remaining;
}
