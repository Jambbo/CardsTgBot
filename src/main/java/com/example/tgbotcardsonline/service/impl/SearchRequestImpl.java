package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.SearchRequest;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.repository.SearchRequestRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchRequestImpl implements SearchRequestService {
    private final SearchRequestRepository searchRequestRepository;
    private final GameService gameService;
    private final TelegramBot telegramBot;
    private final AttackService attackService;

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
            Game game = gameService.createGame(player, opponent);
            // Notify players about the game
            telegramBot.sendMessageToPlayer(player, "Game found! You are playing against " + opponent.getUsername());
            telegramBot.sendMessageToPlayer(opponent, "Game found! You are playing against " + player.getUsername());
            notifyUsersAboutTrump(game.getPlayers(), game);
            attackService.sendMessagesToPlayers(game, game.getActivePlayer());
            searchRequestRepository.delete(searchRequest.get());

        }
    }
    private void notifyUsersAboutTrump(List<OnlinePlayer> players, Game game) {
        Map<String, String> suitSymbols = new HashMap<>();
        suitSymbols.put("HEARTS", "♥");
        suitSymbols.put("DIAMONDS", "♦");
        suitSymbols.put("SPADES", "♠");
        suitSymbols.put("CLUBS", "♣");

        Suit trump = game.getTrump();
        String trumpName = trump.name().toUpperCase();
        String suitSymbol = suitSymbols.get(trumpName);

        players.forEach(
                oP -> {
                    Player player = oP.getPlayer();
                    telegramBot.sendMessageToPlayer(player,"Trump is: "+trump+" "+ suitSymbol);
                }
        );
    }

}
