package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.response.Card;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "Player")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String username;
    @OneToOne
    @JoinColumn(name = "player_in_game_id")
    private OnlinePlayer playerInGame;
    // we can check if user in game by "playerInGame" if it's null - not in game,
    // but I added this for comfort,
    // if u will add method to check if user online without this field than u can delete this
    private boolean inGame;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
