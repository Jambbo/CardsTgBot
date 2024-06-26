package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.*;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.service.validator.MoveValidator;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CardRepository cardRepository;

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
        if (game.getAttacker().equals(onlinePlayer)) {
            if (moveValidator.isAttackMoveValid(game, playerMove)) {
                attackMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't attack with + " + moveValidator.getPrettyMove(playerMove));
            }
        } else if (game.getDefender().equals(onlinePlayer)) {
            if (moveValidator.isDefenceMoveValid(game, playerMove)) {
                defenceMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't defend with + " + moveValidator.getPrettyMove(playerMove));
            }
        } else telegramBot.sendMessageToPlayer(player, "aboba aboba aboba...");
    }


    @Override
    public void attackMove(Game game, Card move) {
        OnlinePlayer attacker = game.getAttacker();
        Player attackerPlayer = attacker.getPlayer();

        updateOnlinePlayerState(attacker, move);

        telegramBot.sendMessageToBothPlayers(game,attackerPlayer.getUsername() + " attacked: " + moveValidator.getPrettyMove(move));
        game.setActivePlayer(game.getDefender());
        game.setOffensiveCard(move);
        gameRepository.save(game);

        if (moveValidator.isPlayerWon(attacker)) {
            nominateWinner(attacker);
        }
    }

    @Override
    public void defenceMove(Game game, Card move) {
        OnlinePlayer defender = game.getDefender();
        Player defenderPlayer = defender.getPlayer();

        telegramBot.sendMessageToBothPlayers(game, defenderPlayer.getUsername() + " defended: " + moveValidator.getPrettyMove(move));

        updateOnlinePlayerState(defender, move);
        List<Card> beaten = game.getBeaten();
        beaten.add(game.getOffensiveCard());
        beaten.add(move);
        game.setOffensiveCard(null);
        game.setBeaten(beaten);
        game.setActivePlayer(game.getAttacker());
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

    @Transactional
    public void takeCards(Player player) {
        boolean possibleToTakeCards = moveValidator.isPossibleToTakeCards(player, player.getPlayerInGame().getGame());
        OnlinePlayer playerInGame = player.getPlayerInGame();
        Game game = playerInGame.getGame();
        List<Card> beaten = game.getBeaten();
        List<Card> playersCards = playerInGame.getCards();
        if (possibleToTakeCards) {
            playersCards.addAll(beaten);
            playersCards.add(game.getOffensiveCard());
            updateStateForTakingCards(game, playerInGame);
            notifyPlayersAfterTakeCards(game);
        } else telegramBot.sendMessageToPlayer(player, "You'r not able to take cards as you'r attacker");
    }

    private void updateStateForTakingCards(Game game, OnlinePlayer playerInGame) {
        game.setActivePlayer(game.getAttacker());
        game.setOffensiveCard(null);
        game.setBeaten(new ArrayList<>());
        refillCards(game);
        onlinePlayerRepository.save(playerInGame);
        gameRepository.save(game);
    }

    private void notifyPlayersAfterTakeCards(Game game) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());
        Player attacker = game.getAttacker().getPlayer();
        Player defender = game.getDefender().getPlayer();
        game.setAttacker(attacker.getPlayerInGame());
        game.setDefender(defender.getPlayerInGame());

        log.info("notify players...");

        telegramBot.sendMessageToBothPlayers(game, defender.getUsername() + " takes the cards!");
        telegramBot.sendMessageToBothPlayers(game, "Now is " + attacker.getUsername() + " move");
        telegramBot.sendMessageToBothPlayers(game, "Remaining cards in the deck: " + deckResponse.getRemaining() + "!");

        telegramBot.showAvailableCards(attacker.getChatId(), attacker.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(defender.getChatId(), defender.getPlayerInGame().getCards());
    }

    private void notifyPLayersAfterFinishAttack(Game game) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());
        Player attacker = game.getAttacker().getPlayer();
        Player defender = game.getDefender().getPlayer();

        log.info("notify players...");

        telegramBot.sendMessageToBothPlayers(game, "Cards beaten and " + attacker.getUsername() + " successfully defended!");
        telegramBot.sendMessageToBothPlayers(game, "Now is " + attacker.getUsername() + " move ");
        telegramBot.sendMessageToBothPlayers(game, "Remaining cards in the deck: " + deckResponse.getRemaining() + "!");

        telegramBot.showAvailableCards(attacker.getChatId(), attacker.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(defender.getChatId(), defender.getPlayerInGame().getCards());
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
        Game game = player.getGame();
        if(player.getCards().remove(move)) {
            log.info("Deleted card " + move + " from player with id: " + player.getId());
            Card cardInDb = cardRepository.findByCode(move.getCode());
            cardInDb.setOnlinePlayer(null);
            cardRepository.save(cardInDb);
            onlinePlayerRepository.save(player);
            gameRepository.save(game);
            log.info("cards players: ");
            player.getCards().forEach(
                    c -> log.info(moveValidator.getPrettyMove(c))
            );
        }else {
            log.info("card "+ move +" was not deleted");
        }
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
