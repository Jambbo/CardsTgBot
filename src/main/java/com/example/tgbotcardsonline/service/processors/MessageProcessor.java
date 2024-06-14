package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final AttackService attackService;

    public void handle(String messageText, Player player){

        if(CardsClient.cards.contains(messageText)){
            if(checkIfPlayersTurn(player)){
                List<Card> cards = player.getPlayerInGame().getCards();
                if(checkIfPlayerHasThisCard(cards, messageText)){

                }
            }
        }
    }

    public boolean checkIfPlayersTurn(Player player){
        Long attackId = 1L;
        Long activePlayerId = attackService.getActivePlayerId(attackId);
        Long oPId = player.getPlayerInGame().getId();
        if(Objects.equals(oPId, activePlayerId)){
            telegramBot.sendMessageToPlayer(player, "Your turn");
            return true;
        }else{
            telegramBot.sendMessageToPlayer(player, "Not your turn.");
            return false;
        }
    }

    public boolean checkIfPlayerHasThisCard(List<Card> cards, String messageText){
        List<Card> list = new ArrayList<>();
        cards.forEach(
                c -> {
                    boolean equals = c.getCode().equals(messageText);
                    if(equals){
                        list.add(c);
                    }
                }
        );
        return !list.isEmpty();
    }

}
