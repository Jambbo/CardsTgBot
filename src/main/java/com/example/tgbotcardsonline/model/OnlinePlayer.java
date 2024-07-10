package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.GameRepository;
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
    @Column(name = "message_id")
    private Integer messageId;
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
            joinColumns = @JoinColumn(name = "online_player_id"),
            inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        if (!cards.contains(card)) {
            cards.add(card);
        } else {
            System.out.println("Duplicate entry for cardId: " + card.getId() + " and onlinePlayerId: " + this.getId());
        }
    }
}
