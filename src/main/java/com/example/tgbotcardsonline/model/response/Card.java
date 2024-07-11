package com.example.tgbotcardsonline.model.response;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.enums.Value;
import com.example.tgbotcardsonline.model.enums.Suit;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Builder
@EqualsAndHashCode
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("code")
    private String code;
    @JsonProperty("image")
    private String image;
    @Enumerated(EnumType.STRING)
    @JsonProperty("value")
    private Value value;
    @Enumerated(EnumType.STRING)
    @JsonProperty("suit")
    private Suit suit;

    public boolean isTrump(Suit trumpSuit) {
        return this.suit == trumpSuit;
    }

    public Card(Suit suit, Value value){
        this.suit = suit;
        this.value = value;
    }

    @Column(name = "game_id")
    private Long gameId;
}
