package com.example.tgbotcardsonline.service.validator;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class MoveValidator {
    private final TelegramBot telegramBot;
    private final DeckResponseRepository deckResponseRepository;

    public boolean isDefenceMoveValid(Game game, Card defendingCard) {
//        Suit trumpSuit = game.getTrump();
//        Card attackingCard = game.getOffensiveCard();
//        if (attackingCard.getSuit().equals(defendingCard.getSuit()) &&
//                defendingCard.getValue().isHigherThan(attackingCard.getValue())) {
//            return true;
//        }
//        return defendingCard.getSuit().equals(trumpSuit) && !attackingCard.getSuit().equals(trumpSuit);
        return true;
    }

    public boolean isAttackMoveValid(Game game, Card playerMove) {
        List<Card> beatenCards = game.getBeaten();
        OnlinePlayer defender = game.getDefender();
        int defenderCardCount = defender.getCards().size();
        int deckSize = game.getCards().size();

        int currentAttackCount = beatenCards.size() / 2 + 1;

        if (deckSize == 24 && currentAttackCount > 5) {
            telegramBot.sendMessageToPlayer(
                    game.getAttacker().getPlayer(),
                    "You are not able to throw more than 5 cards as it is first attack in game. Finish attack."
            );
            return false;
        }

        if (defenderCardCount < 1) {
            telegramBot.sendMessageToPlayer(
                    game.getAttacker().getPlayer(),
                    "Your opponent doesn't have cards. Finish attack."
            );
            return false;
        }

        if (beatenCards.isEmpty()) {
            return true;
        }
        //TODO return to get back new normal game logic
//        return beatenCards.stream().anyMatch(c -> c.getValue().equals(playerMove.getValue()));
        return true;
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
        boolean isAttacker = player.getPlayerInGame().equals(game.getAttacker());
        boolean isFirstMoveNotBeaten = game.getBeaten().isEmpty();
        boolean isOffensiveCardNull = isNull(game.getOffensiveCard());

        if (!isAttacker) {
            telegramBot.sendMessageToPlayer(player, "You are the defender, you are not able to finish the attack.");
            return false;
        }

        if (isFirstMoveNotBeaten) {
            telegramBot.sendMessageToPlayer(player, "You are not able to finish your first move.");
            return false;
        }

        if (isOffensiveCardNull) {
            return true;
        } else {
            telegramBot.sendMessageToPlayer(player,
                    "You are not able to finish the attack. The defender hasn't defended yet. Offensive card: " +
                            getPrettyMove(game.getOffensiveCard())
            );
            return false;
        }
    }

    public boolean isPossibleToTakeCards(Player player, Game game) {
        return !player.getPlayerInGame().equals(game.getAttacker());
    }

    public String getPrettyMove(Card move) {
        Map<String, String> suitSymbols = CardsClient.suitSymbols;

        String cardCode = move.getCode();
        String cardValue = cardCode.startsWith("0") ? "10" : cardCode.substring(0, cardCode.length() - 1);
        String cardSuit = cardCode.substring(cardCode.length() - 1);

        return cardValue + suitSymbols.get(cardSuit);
    }

    public boolean isPlayerWon(OnlinePlayer onlinePlayer) {
        List<Card> cards = onlinePlayer.getCards();
        Game game = onlinePlayer.getGame();
        return cards.isEmpty() && game.getCards().isEmpty();
    }

}
