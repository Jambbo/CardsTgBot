package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final OnlinePlayerService onlinePlayerService;
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final CardService cardService;

    public void handleGameOperation(String messageText, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        if (messageText.startsWith("/")) handleCommandsInGame(messageText, onlinePlayer);
        if (messageText.equals("finish attack")) {

        }
        if(messageText.equals("take cards")){

        }

    }

    public void handleWithMove(String callbackData, Player player) {
        Game game = player.getPlayerInGame().getGame();
        String playerMove = CardsClient.containsCard(callbackData);
        if (isNull(playerMove)) {
            telegramBot.sendMessageToPlayer(player, callbackData + " is not a card!");
        }

        if(!isPlayersMove(player, game)){
            telegramBot.sendMessageToPlayer(player,"It is not your turn.");
            return;
        }

        boolean playerHasThisCard = checkIfPlayerHasThisCard(player.getPlayerInGame().getCards(), callbackData);
        if(!playerHasThisCard){
            telegramBot.sendMessageToPlayer(player,"You do not have this card.");
            return;
        }
        gameService.makeMove(player, playerMove);
    }



    private boolean isPlayersMove(Player player, Game game) {
        OnlinePlayer activePlayerInGame = game.getActivePlayer();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        return activePlayerInGame.equals(onlinePlayer);
    }

    private void handleCommandsInGame(String message, OnlinePlayer player) {
        switch (message) {
            case "/surrend":
                gameService.surrend(player);
                break;
            case "/myCards":
                onlinePlayerService.showMyCards(player);
                break;
            default:
                System.out.println("aboba aboba aboba...");
        }
    }


    public boolean checkIfPlayerHasThisCard(List<Card> cards, String messageText) {
        List<Card> list = new ArrayList<>();
        cards.forEach(
                c -> {
                    boolean equals = c.getCode().equals(messageText);
                    if (equals) {
                        list.add(c);
                    }
                }
        );
        return !list.isEmpty();
    }

}
