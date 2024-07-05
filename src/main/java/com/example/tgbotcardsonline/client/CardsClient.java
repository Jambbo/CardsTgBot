package com.example.tgbotcardsonline.client;

import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.web.mapper.CardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Component
//@RequiredArgsConstructor
@Slf4j
public class CardsClient {
    private Queue<String> deck;
    private final CardMapper cardMapper;
    private final DeckResponseRepository deckResponseRepository;
    public static final List<String> cards= List.of(
            "6S", "7S", "8S", "9S", "0S", "JS", "QS", "KS", "AS",
            "6D", "7D", "8D", "9D", "0D", "JD", "QD", "KD", "AD",
            "6C", "7C", "8C", "9C", "0C", "JC", "QC", "KC", "AC",
            "6H", "7H", "8H", "9H", "0H", "JH", "QH", "KH", "AH"
            );
    public static final Map<String, String> suitSymbols = Map.of(
            "H", "♥",
            "D", "♦",
            "S", "♠",
            "C", "♣"
    );
    public CardsClient(
            CardMapper cardMapper,
            DeckResponseRepository deckResponseRepository
    ) {
        newDeck();
        this.cardMapper = cardMapper;
        this.deckResponseRepository = deckResponseRepository;
    }

    public void newDeck() {
        List<String> deckList = new LinkedList<>(cards);
        Collections.shuffle(deckList);
        deck = new LinkedList<>(deckList);
        log.info("New deck created and shuffled: {}", deck);
    }

    public void shuffleDeck() {
        List<String> deckList = new LinkedList<>(deck);
        Collections.shuffle(deckList);
        deck = new LinkedList<>(deckList);
        log.info("Deck shuffled: {}", deck);
    }

    public List<String> drawCards(int howMany) {
        List<String> drawnCards = new LinkedList<>();
        for (int i = 0; i < howMany; i++) {
            if (deck.isEmpty()) {
                log.warn("No more cards to draw.");
                break;
            }
            drawnCards.add(deck.poll());
        }
        log.info("Cards drawn: {}", drawnCards);
        return drawnCards;
    }

    public DeckResponse contactToPartialDeck(){
        newDeck();
        return DeckResponse.builder()
                .deck_id(UUID.randomUUID().toString())
                .shuffled(true)
                .success(true)
                .remaining(36)
                .build();
    }
    public DrawCardsResponse contactToDrawACard(String deckId,int howMany){
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        int remaining = deckResponse.getRemaining();
        int newRemaining = remaining - howMany;
        deckResponse.setRemaining(newRemaining);
        deckResponseRepository.save(deckResponse);

        List<String> strings = drawCards(howMany);
        List<Card> cards = cardMapper.toCardsFromStringCodes(strings);

        return DrawCardsResponse.builder()
                .cards(cards)
                .success(true)
                .remaining(newRemaining)
                .build();
    }
    public static String containsCard(String input) {
        String normalizedInput = input.replaceAll("\\s+", "").toUpperCase();

        for (String card : cards) {
            if (normalizedInput.contains(card)) {
                return card;
            }
        }
        return null;
    }
}
