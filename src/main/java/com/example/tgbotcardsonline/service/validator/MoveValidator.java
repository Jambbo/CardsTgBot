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
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final CardRepository cardRepository;
    private final GameRepository gameRepository;

    public boolean isDefenceMoveValid(Game game, Card defendingCard) {
        Suit trumpSuit = game.getTrump();
        Card attackingCard = game.getOffensiveCard();

        // If the defending card is of the same suit and has a higher rank
        if (attackingCard.getSuit().equals(defendingCard.getSuit()) &&
                defendingCard.getValue().isHigherThan(attackingCard.getValue())) {
            return true;
        }

        // If the defending card is a trump card and the attacking card is not a trump card
        if (defendingCard.getSuit().equals(trumpSuit) && !attackingCard.getSuit().equals(trumpSuit)) {
            return true;
        }

        // If neither condition is met, the defense move is not valid
        return false;
    }

    public boolean isAttackMoveValid(Game game, Card playerMove) {
        List<Card> beatenCards = game.getBeaten();

        // If there are no beaten cards, it's the first attack, which is always valid
        if (beatenCards.isEmpty()) {
            return true;
        }

        // Subsequent attacking move must match one of the ranks of the current attack
        return beatenCards.stream().anyMatch(c -> c.getValue().equals(playerMove.getValue()));
    }

    public int getValidatedCountToDrawCards(OnlinePlayer player) {
        Game game = player.getGame();
        String deckId = game.getDeckId();
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        return deckResponse.getRemaining();

    }

    public boolean isCardNeeded(OnlinePlayer player) {
        if (player.getCards().size() >= 6) {
            return false;
        }
        return true;
    }

    public boolean isPossibleToDrawCards(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        String deckId = game.getDeckId();
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        int remaining = deckResponse.getRemaining();
        int playerCardsAmount = onlinePlayer.getCards().size();
        int cardsNeeded = 6 - playerCardsAmount;
        if (cardsNeeded > remaining) {
            return false;
        } else {
            return true;
        }
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
        Map<String, String> suitSymbols = new HashMap<>();
        suitSymbols.put("H", "♥");
        suitSymbols.put("D", "♦");
        suitSymbols.put("S", "♠");
        suitSymbols.put("C", "♣");

        String cardCode = move.getCode();
        String cardValue = cardCode.substring(0, cardCode.length() - 1);
        if (cardValue.equals("0")) cardValue = "10";
        String cardSuit = cardCode.substring(cardCode.length() - 1);
        return cardValue + suitSymbols.get(cardSuit);
    }

    public boolean isPlayerWon(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());

        if (onlinePlayer.getCards().isEmpty() && deckResponse.getRemaining() == 0){
            processWinningState(onlinePlayer, game);
            return true;
        }else {
            return false;
        }
    }

    private void processWinningState(OnlinePlayer onlinePlayer, Game game) {
        Player player = onlinePlayer.getPlayer();
        OnlinePlayer attacker = game.getAttacker();
        Player opponent = getOpponent(game, attacker, player);
        processAppStatistics(game, player, opponent);
        processDeletingDataFromDb(game);
        notifyPlayersAboutFinishGame(game);
    }

    private static void processAppStatistics(Game game, Player player, Player opponent) {
        player.setWins(player.getWins()+1);
        opponent.setLosses(player.getLosses()+1);
        game.setWinner(player);
    }

    private void processDeletingDataFromDb(Game game) {
        game.setBeaten(null);
        onlinePlayerRepository.deleteAll(List.of(game.getAttacker(),game.getDefender()));
        deckResponseRepository.delete(deckResponseRepository.findByDeckId(game.getDeckId()));
    }

    private static Player getOpponent(Game game, OnlinePlayer attacker, Player player) {
        return attacker.equals(player) ? game.getDefender().getPlayer() : attacker.getPlayer();
    }

    private void notifyPlayersAboutFinishGame(Game game){
        telegramBot.sendMessageToBothPlayers(game,game.getWinner()+" won!");
    }
}
