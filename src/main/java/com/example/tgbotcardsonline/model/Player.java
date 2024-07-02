package com.example.tgbotcardsonline.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    private boolean inGame;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToOne
    private PlayerStatistics playerStatistics;

}
