package com.example.tgbotcardsonline.client;

import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardsClient {
    private final RestTemplate restTemplate;
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
    public CardsClient() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    public ResponseEntity<DeckResponse> contactToPartialDeck(){
        URI uri = UriComponentsBuilder.fromUriString("https://www.deckofcardsapi.com/api/deck/new/shuffle/")
                .queryParam("cards", String.join(",", cards))
                .build()
                .toUri();

        return restTemplate.getForEntity(uri, DeckResponse.class);
    }
    public DrawCardsResponse contactToDrawACard(String deckId,int howMany){
        RestTemplate restTemplate = new RestTemplate();
        URI uri = UriComponentsBuilder.fromUriString("https://www.deckofcardsapi.com/api/deck/"+deckId+"/draw/?count="+howMany)
                .build()
                .toUri();
        try {
            DrawCardsResponse response = restTemplate.getForObject(uri, DrawCardsResponse.class);
            log.info("Response received: {}", new ObjectMapper().writeValueAsString(response));
            if (response == null || !response.isSuccess()) {
                throw new RuntimeException("Failed to draw cards: response is null or not successful");
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to draw cards from deck {}: {}", deckId, e.getMessage());
            throw new RuntimeException("Failed to draw cards: " + e.getLocalizedMessage(), e);
        }
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
