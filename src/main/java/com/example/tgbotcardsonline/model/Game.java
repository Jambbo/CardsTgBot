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
    @OneToOne
    @Transient
    private Attack currentAttack;
    @Enumerated(EnumType.STRING)
    private Suit trump;
    @OneToOne
    @JoinColumn(name = "active_player_id")
// тоже может быть излишне так как есть в "currentAttack"  но так будет удобнее вроде.
    private OnlinePlayer activePlayer;
    //типо если будет дальше с 3 и 4 игроками, хуй знает может лучше просто пока что два юзера
    @OneToMany
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    List<OnlinePlayer> players;

}
