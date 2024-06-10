package com.example.tgbotcardsonline.client;

import com.example.tgbotcardsonline.model.response.DeckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardsClient {

    public ResponseEntity<DeckResponse> contactToPartialDeck(){
        List<String> cards = List.of(
                "6S", "7S", "8S", "9S", "0S", "JS", "QS", "KS", "AS",
                "6D", "7D", "8D", "9D", "0D", "JD", "QD", "KD", "AD",
                "6C", "7C", "8C", "9C", "0C", "JC", "QC", "KC", "AC",
                "6H", "7H", "8H", "9H", "0H", "JH", "QH", "KH", "AH"
        );
        RestTemplate restTemplate = new RestTemplate();
        URI uri = UriComponentsBuilder.fromUriString("https://www.deckofcardsapi.com/api/deck/new/shuffle/")
                .queryParam("cards", String.join(",", cards))
                .build()
                .toUri();

        return restTemplate.getForEntity(uri, DeckResponse.class);
    }
    public ResponseEntity<DeckResponse> contactToDrawACard(Long deckId){
        RestTemplate restTemplate = new RestTemplate();
        URI uri = UriComponentsBuilder.fromUriString("https://www.deckofcardsapi.com/api/deck/"+deckId+"/draw/?count=1")
                .build()
                .toUri();

        return restTemplate.getForEntity(uri, DeckResponse.class);
    }
}
