package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final AttackService attackService;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final OnlinePlayerService onlinePlayerService;
    private final GameService gameService;
    private final GameRepository gameRepository;

    public void handleGameOperation(String messageText, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        if (messageText.startsWith("/")) handleCommandsInGame(messageText, onlinePlayer);
        if (messageText.equals("finish attack")) {
            attackService.finishAttack(onlinePlayer);
        }
        handleWithMove(messageText, onlinePlayer);
    }

    public void handleGameOperationCallbackData(String callbackData, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
//        if (checkIfPlayersTurn(player)) {
        handleWithMove(callbackData, onlinePlayer);
    }

    private void handleWithMove(String callbackData, OnlinePlayer onlinePlayer) {
        String playerMove = CardsClient.containsCard(callbackData);
        if (isNull(playerMove)) {
            telegramBot.sendMessageToPlayer(onlinePlayer.getPlayer(), callbackData + " is not a card!");
        }
        attackService.makeMove(onlinePlayer, callbackData);

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

//    public boolean checkIfPlayersTurn(Player player) {
//        Long attackId = 1L;
//        Long activePlayerId = attackService.getActivePlayerId(attackId);
//        Long oPId = player.getPlayerInGame().getId();
//        if (Objects.equals(oPId, activePlayerId)) {
//            telegramBot.sendMessageToPlayer(player, "Your turn");
//            return true;
//        } else {
//            telegramBot.sendMessageToPlayer(player, "Not your turn.");
//            return false;
//        }
//    }

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
