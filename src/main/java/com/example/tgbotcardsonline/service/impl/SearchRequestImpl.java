package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.SearchRequest;
import com.example.tgbotcardsonline.repository.SearchRequestRepository;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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
            gameService.createGame(player, searchRequest.get().getSearcher());
        }
    }

}
