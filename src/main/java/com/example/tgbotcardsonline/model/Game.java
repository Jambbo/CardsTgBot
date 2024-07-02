package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "Game")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String deckId;

    @OneToOne
    @JoinColumn(name = "attacker_id")
    private OnlinePlayer attacker;

    @OneToOne
    @JoinColumn(name = "defender_id")
    private OnlinePlayer defender;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "active_player_id")
    private OnlinePlayer activePlayer;

    @Enumerated(EnumType.STRING)
    private Suit trump;

    @OneToOne
    @JoinColumn(name = "offensive_card_id")
    private Card offensiveCard;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "game_beaten",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> beaten;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "game_cards",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cards;

    @OneToOne
    private Player winner;

}
