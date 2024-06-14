package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.CardRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
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

    @Override
    public OnlinePlayer createOnlinePlayer(Player player,String deckId){

        OnlinePlayer onlinePlayer = onlinePlayerMapper.toOnlinePlayer(player);
        DrawCardsResponse drawCardsResponse = getDrawCardsResponse(deckId);
        List<Card> savedCards = cardRepository.saveAll(drawCardsResponse.getCards());
        onlinePlayer.setCards(savedCards);
        onlinePlayerRepository.save(onlinePlayer);
        return onlinePlayer;
    }

    private DrawCardsResponse getDrawCardsResponse(String deckId) {
        DrawCardsResponse drawCardsResponse;
        try {
            drawCardsResponse = cardService.drawACard(deckId, 6);
            if (drawCardsResponse == null || drawCardsResponse.getCards() == null) {
                throw new RuntimeException("DrawCardsResponse or its cards are null");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create online player: " + e.getMessage(), e);
        }
        return drawCardsResponse;
    }
}
