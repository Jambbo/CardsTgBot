package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final CardsClient cardsClient;
    private final CardService cardService;
    private final OnlinePlayerService onlinePlayerService;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final PlayerRepository playerRepository;
    private final TelegramBot telegramBot;

    @Override
    public Game createGame1v1ThrowIn(Player firstPlayer, Player secondPlayer) {
        String deck = cardService.brandNewDeck();
        OnlinePlayer player1 = onlinePlayerService.createOnlinePlayer(firstPlayer, deck);
        OnlinePlayer player2 = onlinePlayerService.createOnlinePlayer(secondPlayer, deck);

        Game game = buildGame(deck, player1, player2);
        Game savedGame = gameRepository.save(game);

        updatePlayerStates(firstPlayer, secondPlayer, player1, player2, savedGame);
        log.info("Successfully created game");
        return savedGame;
    }

    private Game buildGame(String deck, OnlinePlayer player1, OnlinePlayer player2) {
        Suit trump = getRandomTrump();
        OnlinePlayer firstAttacker = countWhoAttackFirst(player1, player2, trump);
        OnlinePlayer defender = firstAttacker.equals(player1) ? player2 : player1;
        return Game.builder()
                .deckId(deck)
                .trump(trump)
                .attacker(firstAttacker)
                .defender(defender)
                .activePlayer(firstAttacker)
                .build();
    }

    private void updatePlayerStates(Player firstPlayer, Player secondPlayer, OnlinePlayer player1, OnlinePlayer player2, Game game) {
        firstPlayer.setInGame(true);
        secondPlayer.setInGame(true);
        firstPlayer.setPlayerInGame(player1);
        secondPlayer.setPlayerInGame(player2);
        playerRepository.saveAll(List.of(firstPlayer, secondPlayer));

        player1.setGame(game);
        player2.setGame(game);
        onlinePlayerRepository.saveAll(List.of(player1, player2));
    }

    @Override
    public void surrend(OnlinePlayer player) {

    }

    @Override
    public void makeMove(Player player, Card playerMove) {
        Game game = player.getPlayerInGame().getGame();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        // if that's attack move
        if (game.getAttacker().equals(onlinePlayer)) {
            //check if valid attack move
            if (isAttackMoveValid(game, playerMove)) {
                attackMove(game,playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't attack with + " + getPrettyMove(playerMove));
                return;
            }
            // if that's defending move
        } else if (game.getDefender().equals(onlinePlayer)) {
            //if that's valid defence move
            if (isDefenceMoveValid(game, playerMove)) {
                defenceMove(game,playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't defend with + " + getPrettyMove(playerMove));
                return;
            }
        } else telegramBot.sendMessageToPlayer(player, "aboba aboba aboba...");
    }


    private boolean isDefenceMoveValid(Game game, Card defendingCard) {
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

    private boolean isAttackMoveValid(Game game, Card playerMove) {
        List<Card> beatenCards = game.getBeaten();

        // If there are no beaten cards, it's the first attack, which is always valid
        if (beatenCards.isEmpty()) {
            return true;
        }

        // Subsequent attacking move must match one of the ranks of the current attack
        return beatenCards.stream().anyMatch(c -> c.getValue().equals(playerMove.getValue()));
    }

    private void attackMove(Game game,Card move) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        updateOnlinePlayerState(attacker, move);
        telegramBot.sendMessageToPlayer(defenderPlayer, attackerPlayer.getUsername()+" attacked: "+getPrettyMove(move));
        telegramBot.sendMessageToPlayer(attackerPlayer, attackerPlayer.getUsername()+" attacked: "+getPrettyMove(move));
        game.setActivePlayer(defender);
        game.setOffensiveCard(move);
        gameRepository.save(game);
    }

    private void defenceMove(Game game,Card move) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        telegramBot.sendMessageToPlayer(defenderPlayer, defenderPlayer.getUsername()+" defended: "+getPrettyMove(move));
        telegramBot.sendMessageToPlayer(attackerPlayer, defenderPlayer.getUsername()+" defended: "+getPrettyMove(move));

        updateOnlinePlayerState(defender, move);
        List<Card> beaten = game.getBeaten();
        beaten.add(game.getOffensiveCard());
        beaten.add(move);
        game.setOffensiveCard(null);
        game.setBeaten(beaten);
        game.setActivePlayer(attacker);
        gameRepository.save(game);
    }

    private String getPrettyMove(Card move){
        Map<String, String> suitSymbols = new HashMap<>();
        suitSymbols.put("H", "♥");
        suitSymbols.put("D", "♦");
        suitSymbols.put("S", "♠");
        suitSymbols.put("C", "♣");

        String cardCode = move.getCode();
        String cardValue = cardCode.substring(0, cardCode.length() - 1);
        if(cardValue.equals("0")) cardValue="10";
        String cardSuit = cardCode.substring(cardCode.length() - 1);
        return cardValue + suitSymbols.get(cardSuit);
    }

    private void updateOnlinePlayerState(OnlinePlayer player, Card move) {
        player.getCards().remove(move);
        onlinePlayerRepository.save(player);
    }

    public Suit getRandomTrump() {
        Suit[] suits = Suit.values();
        Random random = new Random();
        return suits[random.nextInt(suits.length)];
    }

    public OnlinePlayer countWhoAttackFirst(OnlinePlayer player1, OnlinePlayer player2, Suit trump) {
        List<OnlinePlayer> onlinePlayers = List.of(player1, player2);
        return onlinePlayers.stream()
                .flatMap(player -> player.getCards().stream()
                        .filter(card -> card.isTrump(trump))
                        .map(card -> Map.entry(player, card)))
                .min(Comparator.comparing(entry -> entry.getValue().getValue()))
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    Random random = new Random();
                    return onlinePlayers.get(random.nextInt(onlinePlayers.size()));
                });
    }
}
