package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;

import java.util.List;

public interface CardService {

    String brandNewDeck();

    DrawCardsResponse drawACardFromCardsClient(String deckId, int howMany);

    List<Card> drawCards(Game game, int howMany);

    Card getInputtedCardByCodeAndGame(OnlinePlayer onlinePlayer, String callBackData);

}
