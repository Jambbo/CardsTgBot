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
    @OneToOne
    @JoinColumn(name = "offensive_card_id")
    private Card offensiveCard;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "attack_beaten",
            joinColumns = @JoinColumn(name = "attack_id"),
            inverseJoinColumns = @JoinColumn(name = "beaten_card_id")
    )
    private List<Card> beaten;
}
