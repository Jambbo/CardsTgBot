package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.model.response.DrawCardsResponse;
import com.example.tgbotcardsonline.repository.*;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import com.example.tgbotcardsonline.web.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttackServiceImpl implements AttackService {

    private final AttackRepository attackRepository;
    private final TelegramBot telegramBot;
    private final GameRepository gameRepository;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final CardMapper cardMapper;
    private final CardRepository cardRepository;
    private final DeckResponseRepository deckResponseRepository;
    private final CardsClient cardsClient;
    private final CardService cardService;

    public Attack createAttack(Game game) {
        OnlinePlayer attacker = countWhoAttackFirst(game);
        game.setActivePlayer(attacker);
        return Attack.builder()
                .attacker(attacker)
                .defender(getDefender(game))
                .activePlayer(attacker)
                .game(game)
                .build();
    }

    @Override
    public void sendMessagesToPlayers(Game game, OnlinePlayer attacker) {
        game.getPlayers().forEach(oP -> {
            telegramBot.showAvailableCards(oP.getPlayer().getChatId(), oP.getCards());
        });
        telegramBot.sendMessageToPlayer(attacker.getPlayer(), "now is your move!");
        telegramBot.sendMessageToPlayer(getDefender(game).getPlayer(), "now is " + attacker.getPlayer().getUsername() + " move");

    }

    @Override
    public OnlinePlayer countWhoAttackFirst(Game game) {
        List<OnlinePlayer> onlinePlayers = game.getPlayers();
        Suit trump = game.getTrump();
        OnlinePlayer firstAttacker = null;
        Card lowestTrumpCard = null;

        for (OnlinePlayer player : onlinePlayers) {
            for (Card card : player.getCards()) {
                if (card.isTrump(trump)) {
                    if (lowestTrumpCard == null || card.getValue().compareTo(lowestTrumpCard.getValue()) < 0) {
                        lowestTrumpCard = card;
                        firstAttacker = player;
                    }
                }
            }
        }

        if (firstAttacker == null) {
            // No trump cards found, choose randomly
            Random random = new Random();
            firstAttacker = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        }

        return firstAttacker;
    }

    @Override
    public Long getActivePlayerId(Long attackId) {
        return attackRepository.findActivePlayerIdByAttackId(attackId).getId();
    }

    @Override
    public void finishAttack(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        Attack currentAttack = game.getCurrentAttack();
        Card offensiveCards = currentAttack.getOffensiveCard();
        List<Card> defensiveCards = currentAttack.getBeaten();

        // Determine if defender took the cards
        boolean defenderTookCards = (defensiveCards.size() < offensiveCards.size());


        // Clear the current attack
        currentAttack.setOffensiveCard(null);
        currentAttack.setBeaten(null);

        // Switch turns: if the defender successfully defended, he attack next; otherwise, the attacker continues
        if (defenderTookCards) {
            OnlinePlayer nextPlayer = getDefender(game);
            game.setActivePlayer(nextPlayer);
        } else {
            OnlinePlayer nextPlayer = getNextPlayer(game);
            game.setActivePlayer(nextPlayer);
        }

        // Draw cards from the deck if needed
        refillPlayersCardsFromDeck(game);

        gameRepository.save(game);
    }




    private void refillPlayersCardsFromDeck(Game game) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());
        for (OnlinePlayer player : game.getPlayers()) {
            while (player.getCards().size() < 6 && deckResponse.getRemaining() > 0) {
                DrawCardsResponse drawCardsResponse = cardsClient.contactToDrawACard(game.getDeckId(), 1);
                deckResponse.setRemaining(drawCardsResponse.getRemaining());
                Card card = drawCardsResponse.getCards().get(0);
                card.setOnlinePlayer(player);
                player.getCards().add(card);
                // не увверен что надо сохранять карту
                cardRepository.save(card);
                onlinePlayerRepository.save(player);
                deckResponseRepository.save(deckResponse);
            }
        }
    }


    @Override
    @Transactional
    public void makeMove(OnlinePlayer onlinePlayer, String callBackData) {
        Game game = onlinePlayer.getGame();
        Attack currentAttack = game.getCurrentAttack();
        Player currentPlayer = onlinePlayer.getPlayer();
        if (!isPlayerTurn(onlinePlayer)) {
            telegramBot.sendMessageToPlayer(currentPlayer, "It's not your turn!");
            return;
        }
        Card card = cardService.getInputedCard(onlinePlayer, callBackData);
        if (isNull(card)) {
            throw new IllegalArgumentException("Invalid card code: " + callBackData);
        }

        boolean isDefending = onlinePlayer == currentAttack.getDefender();
        try {
            if (isDefending) {
                if (!isDefenseMoveValid(currentAttack, card)) {
                    telegramBot.sendMessageToPlayer(currentPlayer, "Invalid defense move!");
                    return;
                }

                currentAttack.getBeaten().add(card);
                currentAttack.getBeaten().add(currentAttack.getOffensiveCard());

                updateGameStateAfterDefending(game, onlinePlayer, card);
                notifyPlayers(game, onlinePlayer, callBackData);

                if (canFinishAttack(game)) {
                    finishAttack(onlinePlayer);
                } else {
                    notifyPlayers(game, onlinePlayer, callBackData);
                }
            } else {
                if (!isAttackMoveValid(currentAttack, card)) {
                    telegramBot.sendMessageToPlayer(currentPlayer, "Invalid attack move!");
                    return;
                }
                updateGameStateAfterAttack(game, onlinePlayer, card);

                notifyPlayers(game, onlinePlayer, callBackData);
            }
            if (isDefending && currentAttack.getBeaten().isEmpty()) {
                finishAttackDueToNoDefense(game);
            }

            switchTurns(game);
            // точно нужно?
            gameRepository.save(game);

        } catch (Exception e) {
            log.error("Problem in makeMove()");
            e.getLocalizedMessage();
        }
    }

    private void finishAttackDueToNoDefense(Game game) {
        Attack currentAttack = game.getCurrentAttack();
        Card offensiveCard = currentAttack.getOffensiveCard();
        List<Card> beaten = currentAttack.getBeaten();

        // Transfer all cards to the defending player
        OnlinePlayer defendingPlayer = getDefender(game);
        defendingPlayer.getCards().add(offensiveCard);
        defendingPlayer.getCards().addAll(beaten);

        // Clear offensive and defensive cards
        currentAttack.setOffensiveCard(null);
        currentAttack.setBeaten(null);

        // Switch turns to the attacker
        OnlinePlayer nextPlayer = getNextPlayer(game);
        game.setActivePlayer(nextPlayer);

        // Draw cards from the deck if needed
        refillPlayersCardsFromDeck(game);

        gameRepository.save(game);
        onlinePlayerRepository.save(defendingPlayer);
        notifyPlayers(game, defendingPlayer, "defended");
    }

    private void switchTurns(Game game) {
        OnlinePlayer currentPlayer = game.getActivePlayer();
        List<OnlinePlayer> players = game.getPlayers();
        int currentPlayerIndex = players.indexOf(currentPlayer);
        int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
        game.setActivePlayer(players.get(nextPlayerIndex));
        gameRepository.save(game);
    }

    private void handleGameOver(Game game) {
        for (OnlinePlayer player : game.getPlayers()) {
            Player p = player.getPlayer();
            p.setInGame(false);
            telegramBot.sendMessageToPlayer(p, "Game over!");
        }
    }

    //TODO: fix bug with remaining in DeckResponse
    private boolean isGameOver(Game game) {
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(game.getDeckId());
        if (deckResponse.getRemaining() < 1) {
            return game.getPlayers().stream()
                    .anyMatch(player -> player.getCards().isEmpty());
        }
        return false;
    }

    private void notifyPlayers(Game game, OnlinePlayer onlinePlayer, String cardCode) {
        Player currentPlayer = onlinePlayer.getPlayer();
        List<OnlinePlayer> players = game.getPlayers();
        players.forEach(
                oP -> {
                    boolean isOpponent = !oP.equals(currentPlayer);
                    if (isOpponent) {
                        telegramBot.sendMessageToPlayer(oP.getPlayer(), currentPlayer.getUsername() + " played: " + cardCode);
                    }
                }
        );
//        telegramBot.sendMessageToPlayer(currentPlayer, "You played: " + cardCode);
    }

    private void updateGameStateAfterAttack(Game game, OnlinePlayer onlinePlayer, Card card) {
        Attack currentAttack = game.getCurrentAttack();

        // add attack cards
        currentAttack.setOffensiveCard(card);


        //remove player's card
        onlinePlayer.getCards().removeIf(c -> c.getCode().equals(card.getCode()));

        // save !!! (your the worst enemy...)
//        cardRepository.save(card); do we need this ? im not sure
        attackRepository.save(currentAttack);
        onlinePlayerRepository.save(onlinePlayer);
    }

    private void updateGameStateAfterDefending(Game game, OnlinePlayer onlinePlayer, Card card) {
        Attack currentAttack = game.getCurrentAttack();

        // add cards to beaten
        currentAttack.getBeaten().add(card);
        currentAttack.getBeaten().add(currentAttack.getOffensiveCard());

        //remove attacked  and player's card
        onlinePlayer.getCards().removeIf(c -> c.getCode().equals(card.getCode()));
        currentAttack.setOffensiveCard(null);
        card.setOnlinePlayer(null);

        cardRepository.save(card);
        attackRepository.save(currentAttack);
        onlinePlayerRepository.save(onlinePlayer);
    }

    private boolean isValidMove(Game game, OnlinePlayer onlinePlayer, Card card) {
        Attack currentAttack = game.getCurrentAttack();
        if (currentAttack == null) {
            return true; // No current attack, any move is valid
        }

        if (onlinePlayer.equals(game.getActivePlayer())) {
            // Active player's turn to attack
            return isAttackMoveValid(currentAttack, card);
        } else {
            // Defender's turn to defend
            return isDefenseMoveValid(currentAttack, card);
        }
    }

    private boolean isAttackMoveValid(Attack attack, Card card) {
        List<Card> beatenCards = attack.getBeaten();

        // First attacking move is always valid
        if (isNull(beatenCards)) {
            return true;
        }

        // Subsequent attacking move must match one of the ranks of the current attack
        return beatenCards.stream().anyMatch(c -> c.getValue().equals(card.getValue()));
    }

    private boolean isDefenseMoveValid(Attack attack, Card card) {
        Card offensiveCard = attack.getOffensiveCard();
        List<Card> defensiveCards = attack.getBeaten();

        // Check if the card being played can beat the attacking card
        Suit trumpSuit = attack.getGame().getTrump();

        boolean isSameSuitAndHigher = card.getSuit().equals(offensiveCard.getSuit()) && card.getValue().compareTo(offensiveCard.getValue()) > 0;
        boolean isTrumpAndNotTrump = card.getSuit().equals(trumpSuit) && !offensiveCard.getSuit().equals(trumpSuit);

        return isSameSuitAndHigher || isTrumpAndNotTrump;
    }

    private boolean canFinishAttack(Game game) {
        Attack currentAttack = game.getCurrentAttack();
        return (isNull(currentAttack.getOffensiveCard()) && !isNull(currentAttack.getBeaten()));
    }

    public static OnlinePlayer getNextPlayer(Game game) {
        List<OnlinePlayer> players = game.getPlayers();
        OnlinePlayer currentPlayer = game.getActivePlayer();
        int index = players.indexOf(currentPlayer);
        return (index == players.size() - 1) ? players.get(0) : players.get(index + 1);
    }

    public OnlinePlayer getDefender(Game game) {
        List<OnlinePlayer> onlinePlayers = game.getPlayers();
        OnlinePlayer attackFirst = countWhoAttackFirst(game);
        int index = onlinePlayers.indexOf(attackFirst);
        int defenderIndex = (index + 1) % onlinePlayers.size();
        return onlinePlayers.get(defenderIndex);
    }

    private boolean isPlayerTurn(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        return game.getActivePlayer().equals(onlinePlayer);
    }
}
