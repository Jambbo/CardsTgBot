package com.example.tgbotcardsonline.model.response;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Card {
    @Id
    private Long id;
    private String code;
    private String image;
    private String suit;
    private int value;
}
