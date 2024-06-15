package com.example.tgbotcardsonline.model;

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
@Table(name = "Attack")
public class Attack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;
    @OneToOne
    @JoinColumn(name = "attacker_id")
    private OnlinePlayer attacker;
    @OneToOne
    @JoinColumn(name = "defender_id")
    private OnlinePlayer defender;
    @OneToOne
    @JoinColumn(name = "active_player_id")
    // типо кто сейчас ходит
    private OnlinePlayer activePlayer;
    @OneToMany
    // карты которые используються для атаки(не только от атакуещего а еще может кто-то подкинул)
    private List<Card> offensiveCards;
    @OneToMany
    private List<Card> beaten;
}
