package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.CardRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import com.example.tgbotcardsonline.web.mapper.CardMapper;
import com.example.tgbotcardsonline.web.mapper.OnlinePlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnlinePlayerServiceImpl implements OnlinePlayerService {
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final CardService cardService;
    private final OnlinePlayerMapper onlinePlayerMapper;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;


    @Override
    public OnlinePlayer createOnlinePlayer(Player player, String deckId) {
        OnlinePlayer onlinePlayer = onlinePlayerMapper.toOnlinePlayer(player);
        onlinePlayerRepository.save(onlinePlayer);
        DrawCardsResponse drawCardsResponse = getDrawCardsResponseToCreatePlayer(deckId);
        List<Card> newCards = drawCardsResponse.getCards().stream().map(
                card -> cardMapper.toCardFromStringCode(card.getCode())
        ).toList();
        newCards.forEach(card -> {
            card.setOnlinePlayer(onlinePlayer);
            onlinePlayer.addCard(card);
        });
        saveCardsAndPlayerToDb(newCards, onlinePlayer);
        return onlinePlayer;
    }

    private void saveCardsAndPlayerToDb(List<Card> newCards, OnlinePlayer onlinePlayer) {
        cardRepository.saveAll(newCards);
        onlinePlayer.setCards(newCards);
        onlinePlayerRepository.save(onlinePlayer);
    }


    private DrawCardsResponse getDrawCardsResponseToCreatePlayer(String deckId) {
        DrawCardsResponse drawCardsResponse;
        try {
            drawCardsResponse = cardService.drawACardAPI(deckId, 6);
            if (drawCardsResponse == null || drawCardsResponse.getCards() == null) {
                throw new RuntimeException("DrawCardsResponse or its cards are null");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create online player: " + e.getMessage(), e);
        }
        return drawCardsResponse;
    }
}
