package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.response.DrawCardsResponse;

public interface CardService {

    String brandNewDeck();

    DrawCardsResponse drawACard(String deckId, int howMany);

}
