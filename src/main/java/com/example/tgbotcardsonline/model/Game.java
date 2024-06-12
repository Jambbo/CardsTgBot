package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.enums.CardSuit;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "Player")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String deckId;
    @OneToOne
    private Attack currentAttack;
    private CardSuit trump;
    @OneToOne
    @JoinColumn(name = "active_player_id")
// тоже может быть излишне так как есть в "currentAttack"  но так будет удобнее вроде.
    private Player activePlayer;
    //типо если будет дальше с 3 и 4 игроками, хуй знает может лучше просто пока что два юзера
    @OneToMany
    List<OnlinePlayer> players;

}
