package com.example.tgbotcardsonline.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "Player_statistics")
public class PlayerStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long wins;
    private Long gamesPlayed;
    private Double winRate;

    public Double getWinRate(){
        if(gamesPlayed == null || gamesPlayed == 0){
            return 0.0;
        }
        return (double) ((wins*100)/gamesPlayed);
    }
}
