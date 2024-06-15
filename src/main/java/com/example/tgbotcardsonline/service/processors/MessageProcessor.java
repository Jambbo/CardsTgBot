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
        OnlinePlayer onlinePlayer = onlinePlayerRepository.findByPlayer(player).orElseThrow();
        Game game = onlinePlayer.getGame();

        if (messageText.startsWith("/")) handleCommandsInGame(messageText, onlinePlayer);

        if (checkIfPlayersTurn(player)) {
            handleWithMove(messageText, onlinePlayer);
        }

        if (CardsClient.cards.contains(messageText)) {
            if (checkIfPlayersTurn(player)) {
                List<Card> cards = player.getPlayerInGame().getCards();
                if (checkIfPlayerHasThisCard(cards, messageText)) {

                }
            }
        }
    }

    private void handleWithMove(String messageText, OnlinePlayer onlinePlayer) {
        if (messageText.equals("finish attack")) {
            attackService.finishAttack(onlinePlayer);
        } else {
            String playerMove = CardsClient.containsCard(messageText);
            if (isNull(playerMove)){
                telegramBot.sendMessageToPlayer(onlinePlayer.getPlayer(), messageText + " is not a card!");
            }
            attackService.makeMove();
        }
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

    public boolean checkIfPlayersTurn(Player player) {
        Long attackId = 1L;
        Long activePlayerId = attackService.getActivePlayerId(attackId);
        Long oPId = player.getPlayerInGame().getId();
        if (Objects.equals(oPId, activePlayerId)) {
            telegramBot.sendMessageToPlayer(player, "Your turn");
            return true;
        } else {
            telegramBot.sendMessageToPlayer(player, "Not your turn.");
            return false;
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
