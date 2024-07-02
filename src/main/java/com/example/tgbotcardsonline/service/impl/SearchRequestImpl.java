package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.SearchRequest;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.repository.SearchRequestRepository;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchRequestImpl implements SearchRequestService {
    private final SearchRequestRepository searchRequestRepository;
    private final GameService gameService;
    private final TelegramBot telegramBot;

    @Override
    public void StartLookForRandomGame(Player player) {
        if (player.isInGame()) {
            telegramBot.sendMessageToPlayer(player, "Sorry but you already in the game!");
            return;
        }

        Optional<SearchRequest> searchRequest = searchRequestRepository.findOldestRequest();
        if (searchRequest.isEmpty()) { // if No game found - create new search request
            SearchRequest newSearchRequest = SearchRequest.builder()
                    .createdAt(LocalDateTime.now())
                    .searcher(player)
                    .build();
            searchRequestRepository.save(newSearchRequest);
            telegramBot.sendMessageToPlayer(player, "looking for a game!");
        } else {
            Player opponent = searchRequest.get().getSearcher();
//            if (player.getId().equals(opponent.getId())) {
//                telegramBot.sendMessageToPlayer(player, "You already looking for  a game");
//                return;
//            }
            Game game = gameService.createGame1v1ThrowIn(player, opponent);
            notifyUsersAboutStartOfGame(player, opponent, game);
            searchRequestRepository.delete(searchRequest.get());

        }
    }

    private void notifyUsersAboutStartOfGame(Player player, Player opponent, Game game) {
        Player firstAttacker = game.getActivePlayer().getPlayer();
        Player firstDefender = game.getDefender().getPlayer();
        Map<String, String> suitSymbols = Map.of(
                "HEARTS", "♥",
                "DIAMONDS", "♦",
                "SPADES", "♠",
                "CLUBS", "♣"
        );

        Suit trump = game.getTrump();
        String trumpName = trump.name().toUpperCase();
        String suitSymbol = suitSymbols.get(trumpName);

        contactToTelegramBotToSendMessage(player, opponent, trump, suitSymbol, firstAttacker, firstDefender);
    }

    private void contactToTelegramBotToSendMessage(Player player, Player opponent, Suit trump, String suitSymbol, Player firstAttacker, Player firstDefender) {
        telegramBot.sendMessageToPlayer(player, "Trump is: " + trump + " " + suitSymbol);
        telegramBot.sendMessageToPlayer(opponent, "Trump is: " + trump + " " + suitSymbol);
        telegramBot.sendMessageToPlayer(player, "Game found! You are playing against " + opponent.getUsername());
        telegramBot.sendMessageToPlayer(opponent, "Game found! You are playing against " + player.getUsername());
        telegramBot.showAvailableCards(player.getChatId(), player.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(opponent.getChatId(), opponent.getPlayerInGame().getCards());
        telegramBot.sendMessageToPlayer(firstAttacker, "Now is your turn!");
        telegramBot.sendMessageToPlayer(firstDefender, "Now is "+ firstAttacker.getUsername()+" turn");
    }

}
