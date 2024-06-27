package com.example.tgbotcardsonline.service.validator;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.CardRepository;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class MoveValidator {
    private final DeckResponseRepository deckResponseRepository;
    private final TelegramBot telegramBot;

    public boolean isDefenceMoveValid(Game game, Card defendingCard) {
        Suit trumpSuit = game.getTrump();
        Card attackingCard = game.getOffensiveCard();
        if (attackingCard.getSuit().equals(defendingCard.getSuit()) &&
                defendingCard.getValue().isHigherThan(attackingCard.getValue())) {
            return true;
        }
        if (defendingCard.getSuit().equals(trumpSuit) && !attackingCard.getSuit().equals(trumpSuit)) {
            return true;
        }
        return false;
    }

    public boolean isAttackMoveValid(Game game, Card playerMove) {
        List<Card> beatenCards = game.getBeaten();
        if (beatenCards.isEmpty()) {
            return true;
        }
        return beatenCards.stream().anyMatch(c -> c.getValue().equals(playerMove.getValue()));
    }

    public int getValidatedCountToDrawCards(OnlinePlayer player) {
        Game game = player.getGame();
        return game.getCards().size();
    }

    public boolean isCardNeeded(OnlinePlayer player) {
        return player.getCards().size() < 6;
    }

    public boolean isPossibleToDrawCards(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        int remaining = game.getCards().size();
        int playerCardsAmount = onlinePlayer.getCards().size();
        int cardsNeeded = 6 - playerCardsAmount;
        return cardsNeeded <= remaining;
    }

    public boolean isPossibleToFinishMove(Player player, Game game) {
        if(player.getPlayerInGame().equals(game.getAttacker())) {
            List<Card> beaten = game.getBeaten();
            if (beaten.isEmpty()) {
                telegramBot.sendMessageToPlayer(player, "You are not able to finish your first move");
                return false;
            }
            if (isNull(game.getOffensiveCard())) {
                return true;
            } else {
                telegramBot.sendMessageToPlayer(player,
                        " You are not able to finish attack. Defender haven't defended yet. Offensive card: " +
                                getPrettyMove(game.getOffensiveCard())
                );
                return false;
            }
        }
        telegramBot.sendMessageToPlayer(player, "You are defender, you are not able to finish attack.");
        return false;
    }

    public boolean isPossibleToTakeCards(Player player, Game game){
        return !player.getPlayerInGame().equals(game.getAttacker());
    }

    public String getPrettyMove(Card move) {
        Map<String, String> suitSymbols = getSuitSymbolsMap();
        String cardCode = move.getCode();
        String cardValue = cardCode.substring(0, cardCode.length() - 1);
        if (cardValue.equals("0")) cardValue = "10";
        String cardSuit = cardCode.substring(cardCode.length() - 1);
        return cardValue + suitSymbols.get(cardSuit);
    }

    private static Map<String, String> getSuitSymbolsMap() {
        Map<String, String> suitSymbols = new HashMap<>();
        suitSymbols.put("H", "♥");
        suitSymbols.put("D", "♦");
        suitSymbols.put("S", "♠");
        suitSymbols.put("C", "♣");
        return suitSymbols;
    }

    public boolean isPlayerWon(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        return onlinePlayer.getCards().isEmpty() && game.getCards().isEmpty();
    }

}
