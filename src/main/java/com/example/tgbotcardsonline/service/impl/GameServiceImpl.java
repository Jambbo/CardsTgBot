package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.service.validator.MoveValidator;
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
    private final CardService cardService;
    private final OnlinePlayerService onlinePlayerService;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final PlayerRepository playerRepository;
    private final TelegramBot telegramBot;
    private final DeckResponseRepository deckResponseRepository;
    private final MoveValidator moveValidator;

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
            if (moveValidator.isAttackMoveValid(game, playerMove)) {
                attackMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't attack with + " + moveValidator.getPrettyMove(playerMove));
                return;
            }
            // if that's defending move
        } else if (game.getDefender().equals(onlinePlayer)) {
            //if that's valid defence move
            if (moveValidator.isDefenceMoveValid(game, playerMove)) {
                defenceMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't defend with + " + moveValidator.getPrettyMove(playerMove));
                return;
            }
        } else telegramBot.sendMessageToPlayer(player, "aboba aboba aboba...");
    }


    @Override
    public void attackMove(Game game, Card move) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        updateOnlinePlayerState(attacker, move);
        telegramBot.sendMessageToPlayer(defenderPlayer, attackerPlayer.getUsername() + " attacked: " + moveValidator.getPrettyMove(move));
        telegramBot.sendMessageToPlayer(attackerPlayer, attackerPlayer.getUsername() + " attacked: " + moveValidator.getPrettyMove(move));
        game.setActivePlayer(defender);
        game.setOffensiveCard(move);
        gameRepository.save(game);

        if (moveValidator.isPlayerWon(attacker)) {
            nominateWinner(attacker);
        }
    }

    @Override
    public void defenceMove(Game game, Card move) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        telegramBot.sendMessageToBothPlayers(game, defenderPlayer.getUsername() + " defended: " + moveValidator.getPrettyMove(move));

        updateOnlinePlayerState(defender, move);
        List<Card> beaten = game.getBeaten();
        beaten.add(game.getOffensiveCard());
        beaten.add(move);
        game.setOffensiveCard(null);
        game.setBeaten(beaten);
        game.setActivePlayer(attacker);
        gameRepository.save(game);

        if (moveValidator.isPlayerWon(defender)) {
            nominateWinner(defender);
        }
    }

    @Override
    public void finishAttack(Player player, Game game) {
        boolean possibleToFinishMove = moveValidator.isPossibleToFinishMove(player, game);
        log.info(player.getUsername() + " trying to finish attack. That's possible ? =" + possibleToFinishMove);
        if (possibleToFinishMove) {
            game.setBeaten(new ArrayList<>());

            switchTurnsAtFinishAttack(game);

            refillCards(game);

            gameRepository.save(game);

            notifyPLayersAfterFinishAttack(game);
        }
    }

    public void takeCards(Player player) {

    }

    private void notifyPLayersAfterFinishAttack(Game game) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());
        Player attacker = game.getAttacker().getPlayer();
        Player defender = game.getDefender().getPlayer();

        log.info("notify players...");

        telegramBot.sendMessageToBothPlayers(game, "Cards beaten and " + attacker.getUsername() + " successfully defended!");
        telegramBot.sendMessageToBothPlayers(game, "Now is " + attacker.getUsername() + " move ");
        telegramBot.sendMessageToBothPlayers(game, "Remaining cards in the deck: " + deckResponse.getRemaining() + "!");

        telegramBot.showAvailableCards(attacker.getId(), attacker.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(defender.getId(), defender.getPlayerInGame().getCards());
    }

    private void refillCards(Game game) {

        OnlinePlayer attackerWithRefilledCards = refillCardsToPlayer(game.getAttacker());
        OnlinePlayer defenderWithRefilledCards = refillCardsToPlayer(game.getDefender());


        if (attackerWithRefilledCards.getCards().isEmpty()) {
            nominateWinner(attackerWithRefilledCards);
            return;
        }
        if (defenderWithRefilledCards.getCards().isEmpty()) {
            nominateWinner(defenderWithRefilledCards);
        }
    }

    private void nominateWinner(OnlinePlayer attackerWithRefilledCards) {
        log.info(attackerWithRefilledCards.getPlayer().getUsername() + " WONNN ABOBABOABOAOBOABOABOA");
    }

    private OnlinePlayer refillCardsToPlayer(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        if (moveValidator.isCardNeeded(onlinePlayer)) {
            if (moveValidator.isPossibleToDrawCards(onlinePlayer)) {
                cardService.drawACard(game.getDeckId(), 6 - onlinePlayer.getCards().size());
            } else {
                int validatedCountToDrawCards = moveValidator.getValidatedCountToDrawCards(onlinePlayer);
                cardService.drawACard(game.getDeckId(), validatedCountToDrawCards);
            }
        }
        log.info(onlinePlayer.getPlayer().getUsername() + "cards:  " + onlinePlayer.getCards());
        onlinePlayerRepository.save(onlinePlayer);
        return onlinePlayer;
    }

    private void switchTurnsAtFinishAttack(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        game.setActivePlayer(defender);
        game.setAttacker(defender);
        game.setDefender(attacker);
        log.info("last attacker was : " + attacker);
        log.info("new attacker is: " + defender);
        gameRepository.save(game);
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
