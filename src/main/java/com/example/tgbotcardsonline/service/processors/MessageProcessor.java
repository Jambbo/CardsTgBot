package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final OnlinePlayerService onlinePlayerService;
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final CardService cardService;
    private final DeckResponseRepository deckResponseRepository;

    public void handleGameOperation(String messageText, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        Game game = onlinePlayer.getGame();
        switch (messageText) {
            case "resign":
                gameService.surrend(onlinePlayer);
                break;
            case "myCards":
                onlinePlayerService.showMyCards(onlinePlayer);
                break;
            default:
                log.info(player.getUsername()+ " wrote: " + messageText);
        }

        boolean isPlayersTurn = isPlayersMove(player, game);
        if (isPlayersTurn) {
            switch (messageText) {
                case "finish attack" -> gameService.finishAttack(player, game);
                case "take cards" -> gameService.takeCards(player);
                default -> telegramBot.sendMessageToPlayer(player, "Unknown command.");
            }
        }

    }

    public void handleWithMove(String callbackData, Player player) {
        Game game = player.getPlayerInGame().getGame();
        String playerMove = CardsClient.containsCard(callbackData);
        if (isNull(playerMove)) {
            telegramBot.sendMessageToPlayer(player, callbackData + " is not a card!");
        }

        if (!isPlayersMove(player, game)) {
            telegramBot.sendMessageToPlayer(player, "It is not your turn.");
            return;
        }

        Card playersCard = checkIfPlayerHasThisCard(player.getPlayerInGame().getCards(), callbackData);
        if (playersCard == null) {
            telegramBot.sendMessageToPlayer(player, "You do not have this card.");
            return;
        }
        gameService.makeMove(player, playersCard);
    }

    private boolean isPlayersMove(Player player, Game game) {
        OnlinePlayer activePlayerInGame = game.getActivePlayer();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        return activePlayerInGame.equals(onlinePlayer);
    }

    private void handleCommandsInGame(String message, OnlinePlayer player) {

    }


    public Card checkIfPlayerHasThisCard(List<Card> cards, String messageText) {
        for (Card card : cards) {
            if (card.getCode().equals(messageText)) {
                return card;
            }
        }
        return null;
    }

}
