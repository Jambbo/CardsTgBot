package com.example.tgbotcardsonline.service;

public interface CardService {

    String brandNewDeck();

    void drawACard(String deckId, int howMany);
}
