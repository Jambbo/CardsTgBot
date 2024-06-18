package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.enums.Suit;
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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "attack_id")
    private Attack currentAttack;
    @Enumerated(EnumType.STRING)
    private Suit trump;
    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "active_player_id")
    private OnlinePlayer activePlayer;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    List<OnlinePlayer> players;

}
