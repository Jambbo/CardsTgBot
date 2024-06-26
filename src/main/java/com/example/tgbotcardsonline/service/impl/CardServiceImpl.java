package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardsClient cardsClient;
    private final DeckResponseRepository deckResponseRepository;
    private final TelegramBot telegramBot;

    @Override
    public String brandNewDeck() {
        ResponseEntity<DeckResponse> deckResponseResponseEntity = cardsClient.contactToPartialDeck();
        DeckResponse deckResponse = deckResponseResponseEntity.getBody();
        deckResponseRepository.save(deckResponse);
        log.info(deckResponse.toString());
        return deckResponse.getDeck_id();
    }

    @Override
    public DrawCardsResponse drawACard(String deckId, int howMany) {
        try {
            DrawCardsResponse drawCardsResponse = cardsClient.contactToDrawACard(deckId, howMany);
            DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
            deckResponse.setRemaining(drawCardsResponse.getRemaining());
            deckResponseRepository.save(deckResponse);
            return drawCardsResponse;
        } catch (Exception e) {
            throw new RuntimeException("Error drawing cards: " + e.getMessage(), e);
        }
    }

    @Override
    public Card getInputedCard(OnlinePlayer onlinePlayer, String callBackData) {

        for (Card card : onlinePlayer.getCards()) {
            if (card.getCode().equals(callBackData)) {
                return card;
            }
        }
        telegramBot.sendMessageToPlayer(onlinePlayer.getPlayer(), "You do not have card " + callBackData);
        throw new RuntimeException();
    }
}
