package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnlinePlayerServiceImpl implements OnlinePlayerService {
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final CardService cardService;

    public OnlinePlayer createOnlinePlayer(Player player,String deckId){
        OnlinePlayer onlinePlayer = OnlinePlayer.builder()
                .player(player)
//                .cards(cardService.drawACard(deckId,6))
                .build();
        return onlinePlayer;
    }
}
