package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.CardSuit;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final CardsClient cardsClient;
    private final CardService cardService;

    @Override
    public Game createGame(Player firstPlayer, Player secondPlayer) {
        // создать Online players

        Game game = Game.builder()
                .deckId(cardService.brandNewDeck())
                .trump(getRandomTrump())
//                .players()
                .build();
        return gameRepository.save(game);
    }

    public CardSuit getRandomTrump() {
        CardSuit[] suits = CardSuit.values();
        Random random = new Random();
        return suits[random.nextInt(suits.length)];
    }

}
