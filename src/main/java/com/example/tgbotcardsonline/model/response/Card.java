package com.example.tgbotcardsonline.model.response;

import lombok.Data;

@Data
public class Card {
    private String code;
    private String image;
    private String suit;
    private int value;
}
