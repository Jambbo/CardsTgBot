package com.example.tgbotcardsonline.model;

import com.example.tgbotcardsonline.model.enums.GameType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "search_request")
public class SearchRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "searcher_id")
    private Player searcher;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    private GameType gameType;
}
