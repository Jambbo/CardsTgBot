package com.example.tgbotcardsonline.service.impl;

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
import com.example.tgbotcardsonline.service.processors.WinProcessor;
import com.example.tgbotcardsonline.service.validator.MoveValidator;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final WinProcessor winProcessor;

    @Override
    public Game createGame1v1ThrowIn(Player firstPlayer, Player secondPlayer) {
        String deck = cardService.brandNewDeck();
        OnlinePlayer player1 = onlinePlayerService.createOnlinePlayer(firstPlayer, deck);
        OnlinePlayer player2 = onlinePlayerService.createOnlinePlayer(secondPlayer, deck);

        Game game = buildGame(deck, player1, player2);
        Game savedGame = gameRepository.save(game);

        updatePlayerStates(savedGame);
        log.info("Successfully created game");
        return savedGame;
    }

    private Game buildGame(String deckId, OnlinePlayer player1, OnlinePlayer player2) {
        List<Card> cards = getCards(deckId);
        Card card = cards.get(cards.size() - 1);
        Suit trump = card.getSuit();
        OnlinePlayer firstAttacker = countWhoAttackFirst(player1, player2, trump);
        OnlinePlayer defender = firstAttacker.equals(player1) ? player2 : player1;
        telegramBot.sendMessageToPlayer(player1.getPlayer(), "Trump card is:  " + moveValidator.getPrettyMove(card));
        telegramBot.sendMessageToPlayer(player2.getPlayer(), "Trump card is:  " + moveValidator.getPrettyMove(card));
        return Game.builder()
                .deckId(deckId)
                .trump(trump)
                .attacker(firstAttacker)
                .defender(defender)
                .activePlayer(firstAttacker)
                .cards(cards)
                .winner(null)
                .build();
    }

    private List<Card> getCards(String deckId) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        int remaining = deckResponse.getRemaining();
        List<Card> cards = cardService.drawACardFromCardsClient(deckId, remaining).getCards();
        cardRepository.saveAll(cards);
        return cards;
    }


    private void updatePlayerStates(Game game) {
        OnlinePlayer onlinePlayer1 = game.getAttacker();
        OnlinePlayer onlinePlayer2 = game.getDefender();
        Player firstPlayer = onlinePlayer1.getPlayer();
        Player secondPlayer = onlinePlayer2.getPlayer();

        setPlayersInGame(firstPlayer, secondPlayer, onlinePlayer1, onlinePlayer2);
        setGameToOnlinePlayers(game);

        setGameToCards(game);
        setGameToPlayerCards(onlinePlayer1);
        setGameToPlayerCards(onlinePlayer2);
    }

    private void setGameToOnlinePlayers(Game game) {
        OnlinePlayer onlinePlayer1 = game.getAttacker();
        OnlinePlayer onlinePlayer2 = game.getDefender();
        onlinePlayer1.setGame(game);
        onlinePlayer2.setGame(game);
        onlinePlayerRepository.saveAll(List.of(onlinePlayer1, onlinePlayer2));
    }

    private void setPlayersInGame(Player firstPlayer, Player secondPlayer, OnlinePlayer player1, OnlinePlayer player2) {
        firstPlayer.setInGame(true);
        secondPlayer.setInGame(true);
        firstPlayer.setPlayerInGame(player1);
        secondPlayer.setPlayerInGame(player2);
        playerRepository.saveAll(List.of(firstPlayer, secondPlayer));
    }

    private void setGameToCards(Game game) {
        List<Card> cards = game.getCards();
        cards.forEach(c -> c.setGameId(game.getId()));
        cardRepository.saveAll(cards);
    }

    private void setGameToPlayerCards(OnlinePlayer player1) {
        List<Card> cards = player1.getCards();
        cards.forEach(c -> c.setGameId(player1.getGame().getId()));
        cardRepository.saveAll(cards);
    }

    @Override
    public void resign(OnlinePlayer player) {
        Player winner = winProcessor.getWinnerDuringResign(player);
        winProcessor.processWinningState(player);
        telegramBot.sendMessageToPlayer(
                winner,
                player.getPlayer().getUsername() + " gave up!" + "\nWinner: " + winner.getUsername()
        );
        telegramBot.sendMessageToPlayer(
                player.getPlayer(),
                player.getPlayer().getUsername() + " gave up!" + "\nWinner: " + winner.getUsername()
        );
    }

    @Override
    public void makeMove(Player player, Card playerMove) {
        Game game = player.getPlayerInGame().getGame();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        if (game.getAttacker().equals(onlinePlayer)) {
            if (moveValidator.isAttackMoveValid(game, playerMove)) {
                attackMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't attack with " + moveValidator.getPrettyMove(playerMove));
            }
        } else if (game.getDefender().equals(onlinePlayer)) {
            if (moveValidator.isDefenceMoveValid(game, playerMove)) {
                defenceMove(game, playerMove);
            } else {
                telegramBot.sendMessageToPlayer(player, "You can't defend with " + moveValidator.getPrettyMove(playerMove));
            }
        } else telegramBot.sendMessageToPlayer(player, "aboba aboba aboba...");
    }


    @Override
    @SneakyThrows
    public void attackMove(Game game, Card move) {
        OnlinePlayer attacker = game.getAttacker();
        Player attackerPlayer = attacker.getPlayer();

        Integer messageIdForOpponent = telegramBot.sendMessageToPlayer(game.getDefender().getPlayer(), attackerPlayer.getUsername() + " attacked: " + moveValidator.getPrettyMove(move)).get();
        Integer messageId = telegramBot.sendMessageToPlayer(attackerPlayer, "You attacked: " + moveValidator.getPrettyMove(move)).get();

        attacker.setMessageId(messageId);
        attacker.setMessageIdSentToOpponent(messageIdForOpponent);
        updateOnlinePlayerState(attacker, move);

        game.setActivePlayer(game.getDefender());
        game.setOffensiveCard(move);
        gameRepository.save(game);

        if (moveValidator.isPlayerWon(attacker)) {
            nominateWinner(attacker);
            return;
        }
        telegramBot.updateAvailableCards(attacker, attacker.getCards());
        telegramBot.updateNowMoveMessages(game);
    }

    @Override
    @SneakyThrows
    public void defenceMove(Game game, Card move) {
        OnlinePlayer defender = game.getDefender();
        Player defenderPlayer = defender.getPlayer();

        Integer messageIdForOpponent = telegramBot.sendMessageToPlayer(game.getAttacker().getPlayer(), defenderPlayer.getUsername() + " defended with: " + moveValidator.getPrettyMove(move)).get();
        Integer messageId = telegramBot.sendMessageToPlayer(defenderPlayer, "You defended with: " + moveValidator.getPrettyMove(move)).get();

        defender.setMessageId(messageId);
        defender.setMessageIdSentToOpponent(messageIdForOpponent);
        updateOnlinePlayerState(defender, move);

        List<Card> beaten = game.getBeaten();
        beaten.add(game.getOffensiveCard());
        beaten.add(move);
        game.setOffensiveCard(null);
        game.setBeaten(beaten);
        game.setActivePlayer(game.getAttacker());
        gameRepository.save(game);
        onlinePlayerRepository.save(defender);

        if (moveValidator.isPlayerWon(defender)) {
            nominateWinner(defender);
            return;
        }
        telegramBot.updateAvailableCards(defender, defender.getCards());
        telegramBot.updateBeatenCardsMessages(game);
        telegramBot.updateNowMoveMessages(game);
        checkOnFinishAttack(game);
    }

    private void checkOnFinishAttack(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        List<Card> attackerCards = attacker.getCards();
        List<Card> defenderCards = defender.getCards();
        if (attackerCards.isEmpty() || defenderCards.isEmpty()) {
            finishAttack(attacker.getPlayer(), game);
        }
    }

    @Override
    public void finishAttack(Player player, Game game) {
        boolean possibleToFinishMove = moveValidator.isPossibleToFinishMove(player, game);
        log.info(player.getUsername() + " trying to finish attack. That's possible ? =" + possibleToFinishMove);
        if (possibleToFinishMove) {
            game.setBeaten(new ArrayList<>());
            refillCards(game);
            switchTurnsAtFinishAttack(game);
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
            updateStateForTakingCards(game);
            notifyPlayersAfterTakeCards(game);
        } else telegramBot.sendMessageToPlayer(player, "You'r not able to take cards as you'r attacker");
    }

    private void updateStateForTakingCards(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        attacker.setMessageIdNowMove(null);
        attacker.setBeatenCardsMessageId(null);
        defender.setMessageIdNowMove(null);
        defender.setBeatenCardsMessageId(null);
        game.setActivePlayer(attacker);
        game.setOffensiveCard(null);
        game.setBeaten(new ArrayList<>());
        refillCards(game);
        onlinePlayerRepository.save(attacker);
        onlinePlayerRepository.save(defender);
        gameRepository.save(game);
    }

    private void notifyPlayersAfterTakeCards(Game game) {
        Player attacker = game.getAttacker().getPlayer();
        Player defender = game.getDefender().getPlayer();
        game.setAttacker(attacker.getPlayerInGame());
        game.setDefender(defender.getPlayerInGame());

        log.info("notify players...");

        telegramBot.sendMessageToBothPlayers(game, defender.getUsername() + " takes the cards!");
//        telegramBot.sendMessageToBothPlayers(game, "Now is " + attacker.getUsername() + " move");
        telegramBot.sendMessageToBothPlayers(game, "Remaining cards in the deck: " + game.getCards().size() + "!");

        telegramBot.showAvailableCards(attacker.getPlayerInGame(), attacker.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(defender.getPlayerInGame(), defender.getPlayerInGame().getCards());
        telegramBot.updateNowMoveMessages(game);
    }

    private void notifyPLayersAfterFinishAttack(Game game) {
        Player attacker = game.getAttacker().getPlayer();
        Player defender = game.getDefender().getPlayer();

        log.info("notify players...");

        telegramBot.sendMessageToBothPlayers(game, "Cards beaten and " + attacker.getUsername() + " successfully defended!");
//        telegramBot.sendMessageToBothPlayers(game, "Now is " + attacker.getUsername() + " move ");
        telegramBot.sendMessageToBothPlayers(game, "Remaining cards in the deck: " + game.getCards().size() + "!");

        telegramBot.showAvailableCards(attacker.getPlayerInGame(), attacker.getPlayerInGame().getCards());
        telegramBot.showAvailableCards(defender.getPlayerInGame(), defender.getPlayerInGame().getCards());
        telegramBot.updateNowMoveMessages(game);
    }

    private void refillCards(Game game) {
        refillCardsToPlayer(game.getAttacker());
        refillCardsToPlayer(game.getDefender());

        gameRepository.save(game);
    }

    private void nominateWinner(OnlinePlayer winner) {
        Game game = winner.getGame();
        Player playerWinner = winner.getPlayer();
        OnlinePlayer loser =
                game.getAttacker().equals(winner) ?
                        game.getDefender() : game.getAttacker();
        Player loserPlayer = loser.getPlayer();
        winProcessor.processWinningState(loser);
        telegramBot.sendMessageToPlayer(playerWinner, playerWinner.getUsername() + " - won!");
        telegramBot.sendMessageToPlayer(loserPlayer, playerWinner.getUsername() + " - won!");
    }

    private void refillCardsToPlayer(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        if (!moveValidator.isCardNeeded(onlinePlayer)) {
            log.info(
                    onlinePlayer.getPlayer().getUsername() + " doesn't need any cards "
            );
            return;
        }
        int cardsToDraw = moveValidator.isPossibleToDrawCards(onlinePlayer) ?
                6 - onlinePlayer.getCards().size() :
                moveValidator.getValidatedCountToDrawCards(onlinePlayer);
        List<Card> cards = cardService.drawCards(game, cardsToDraw);
        if (cardsToDraw > 0 && cards.isEmpty()) {
            log.warn("Expected to draw " + cardsToDraw + " cards but received none.");
        }
        addCardsToPlayer(onlinePlayer, cards);

        log.info(onlinePlayer.getPlayer().getUsername() + "cards:  " + onlinePlayer.getCards());

        gameRepository.save(game);
    }


    private void addCardsToPlayer(OnlinePlayer onlinePlayer, List<Card> cards) {
        if (cards.isEmpty()) {
            log.info("No cards to add for onlinePlayerId: " + onlinePlayer.getId());
            return;
        }
        onlinePlayer.getCards().addAll(cards);
    }

    private void switchTurnsAtFinishAttack(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();

        attacker.setMessageIdNowMove(null);
        attacker.setBeatenCardsMessageId(null);

        defender.setMessageIdNowMove(null);
        defender.setBeatenCardsMessageId(null);

        game.setActivePlayer(defender);
        game.setAttacker(defender);
        game.setDefender(attacker);
        log.info("last attacker was : " + attacker);
        log.info("new attacker is: " + defender);
        onlinePlayerRepository.save(attacker);
        onlinePlayerRepository.save(defender);
        gameRepository.save(game);
    }

    private void updateOnlinePlayerState(OnlinePlayer player, Card move) {
        Game game = player.getGame();
        if (player.getCards().remove(move)) {
            log.info("Deleted card " + move + " from player with id: " + player.getId());
            Card card = cardService.getInputtedCardByCodeAndGame(player, move.getCode());
            savePlayerAndCardAndGameToDb(player, card, game);
            log.info("cards players: ");
            player.getCards().forEach(
                    c -> log.info(moveValidator.getPrettyMove(c))
            );
        } else {
            log.info("card " + move + " was not deleted");
        }
    }

    private void savePlayerAndCardAndGameToDb(OnlinePlayer player, Card card, Game game) {
        cardRepository.save(card);
        onlinePlayerRepository.save(player);
        gameRepository.save(game);
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
