package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.service.CardService;
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
            return cardsClient.contactToDrawACard(deckId, howMany);
        } catch (Exception e) {
            throw new RuntimeException("Error drawing cards: " + e.getMessage(), e);
        }
    }

    @SneakyThrows
    @Override
    public Card getInputedCard(OnlinePlayer onlinePlayer, String callBackData) {
        return onlinePlayer.getCards().stream()
                .filter(
                        c -> c.getCode().equals(callBackData)
                ).findFirst().get();
    }
}
