package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.PlayerStatistics;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final GameService gameService;


    public void handleGameOperation(String messageText, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        Game game = onlinePlayer.getGame();
        switch (messageText) {
            case "resign" -> {
                gameService.resign(onlinePlayer);
                return;
            }
            case "/myprofile" -> {
                handleMyProfileQuery(player);
                return;
            }
            case "/help" -> {
                handleHelpQuery(player);
                return;
            }
            default -> log.info(player.getUsername()+ " wrote: " + messageText);
        }

        boolean isPlayersTurn = isPlayersMove(player, game);
        if (isPlayersTurn) {
            switch (messageText) {
                case "finish attack" -> gameService.finishAttack(player, game);
                case "take cards" -> gameService.takeCards(player);
                default -> telegramBot.sendMessageToPlayer(player, "Unknown command.");
            }
        }else{
            telegramBot.sendMessageToPlayer(player,"It's not your turn.");
        }

    }

    public void handleHelpQuery(Player player) {
        String helpMessage = "🆘 *Bot Commands* 🆘\n\n" +
                "/startgame - Start a new game with a random player\n" +
                "/myprofile - Open your profile\n" +
                "/help - Bot usage guide\n";

        telegramBot.sendMessageToPlayer(player, helpMessage);
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

    public void handleMyProfileQuery(Player player) {
        PlayerStatistics playerStatistics = player.getPlayerStatistics();
        Long gamesPlayed = playerStatistics.getGamesPlayed();
        Long wins = playerStatistics.getWins();
        Long losses = gamesPlayed - wins;
        Double winRate = playerStatistics.getWinRate();

        String message = String.format(
                """
                        📊 *Your Stats* 📊

                        🏅 *Games Played:* %d
                        🏆 *Games Won:* %d
                        ❌ *Games Lost:* %d
                        📈 *Win Rate:* %.2f%%""",
                gamesPlayed, wins, losses, winRate
        );

        telegramBot.sendMessageToPlayer(player, message);
    }

    private boolean isPlayersMove(Player player, Game game) {
        OnlinePlayer activePlayerInGame = game.getActivePlayer();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        return activePlayerInGame.equals(onlinePlayer);
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
