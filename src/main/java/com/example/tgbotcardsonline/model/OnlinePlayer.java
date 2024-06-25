package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.response.Card;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "online_player")
public class OnlinePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private Game game;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "op_cards",
            joinColumns =@JoinColumn(name = "online_player_id"),
            inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
        card.setOnlinePlayer(this);
    }
    public void removeCard(Card card){
        cards.remove(card);
    }
}
