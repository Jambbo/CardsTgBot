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
@Table(name = "OnlinePlayer")
public class OnlinePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
    // Вот тут что-то нихуя не уверен, чекни может оно нахуй не нужно
    @OneToMany
    private List<Card> cards;
}
