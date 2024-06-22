package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import lombok.SneakyThrows;

public interface CardService {

    String brandNewDeck();

    DrawCardsResponse drawACard(String deckId, int howMany);

    Card getInputedCard(OnlinePlayer onlinePlayer, String callBackData);
}
