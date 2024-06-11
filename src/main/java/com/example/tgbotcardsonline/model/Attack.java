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
    /*
    *
    Это не отбой. просто тут будут храниться карты которые побили, еще не обьязательно до конца.
     К примеру на тебя кинули 2 шестерки и одну ты побил и теперь в этом листе карта которую ты побил и то чем ты побил,
     к примеру вторую шестерку ты побить не смог  и теперь возвращаються тебе все карты которые в этом листе и которые в листе выше.
     *
     */
    private List<Card> beaten;
}
