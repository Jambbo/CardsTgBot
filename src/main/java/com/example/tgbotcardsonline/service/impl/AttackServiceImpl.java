package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.repository.CardRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import com.example.tgbotcardsonline.web.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private final AttackRepository attackRepository;
    private final TelegramBot telegramBot;
    private final GameRepository gameRepository;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final CardMapper cardMapper;
    private final CardRepository cardRepository;

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
//        telegramBot.showAvailableCards(attacker.getPlayer().getChatId(), attacker.getCards());
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
        AtomicReference<OnlinePlayer> firstAttacker = new AtomicReference<>(null);
        AtomicReference<Card> lowestTrumpCard = new AtomicReference<>(null);
        onlinePlayers.forEach(oP -> {
            oP.getCards().forEach(c -> {
                c.getSuit();
                boolean isTrump = c.isTrump(trump);
                if (lowestTrumpCard.get() == null || c.getValue().compareTo(lowestTrumpCard.get().getValue()) < 6) {
                    lowestTrumpCard.set(c);
                    firstAttacker.set(oP);
                }
            });
        });
        return firstAttacker.get();
    }

    @Override
    public Long getActivePlayerId(Long attackId) {
        return attackRepository.findActivePlayerIdByAttackId(attackId).getId();
    }

    @Override
    public void finishAttack(OnlinePlayer onlinePlayer) {

    }

    @Override
    @Transactional
    public void makeMove(OnlinePlayer onlinePlayer, String callBackData) {
        Game game = onlinePlayer.getGame();
        Player currentPlayer = onlinePlayer.getPlayer();
        if (!isPlayerTurn(onlinePlayer)) {
            telegramBot.sendMessageToPlayer(currentPlayer, "It's not your turn!");
            return;
        }
        Card card = cardRepository.findByCode(callBackData);
        if(isNull(card)){
            throw new IllegalArgumentException("Invalid card code: " + callBackData);
        }
        updateGameState(game, onlinePlayer, card);
        notifyPlayers(game, onlinePlayer, callBackData);
        if (isGameOver(game)) {
            handleGameOver(game);
        } else {
            // Switch turns
            switchTurns(game);
        }
        gameRepository.save(game);
    }

    private void switchTurns(Game game) {
        OnlinePlayer currentPlayer = game.getActivePlayer();
        OnlinePlayer nextPlayer = game.getPlayers().stream()
                .filter(oP -> !oP.equals(currentPlayer))
                .findFirst()
                .orElseThrow();

        game.setActivePlayer(nextPlayer);
        Attack attack = attackRepository.findByGame(game);
        attack.setActivePlayer(nextPlayer);
        attack.setAttacker(nextPlayer);
        attack.setDefender(currentPlayer);
    }

    private void handleGameOver(Game game) {
        for (OnlinePlayer player : game.getPlayers()) {
            Player p = player.getPlayer();
            p.setInGame(false);
            telegramBot.sendMessageToPlayer(p, "Game over!");
        }
    }

    private boolean isGameOver(Game game) {
        return game.getPlayers().stream()
                .anyMatch(player -> player.getCards().isEmpty());
    }

    private void notifyPlayers(Game game, OnlinePlayer onlinePlayer, String cardCode) {
        Player currentPlayer = onlinePlayer.getPlayer();
        OnlinePlayer opponent = game.getPlayers().stream()
                .filter(oP -> !oP.equals(currentPlayer))
                .findFirst()
                .orElseThrow();

        telegramBot.sendMessageToPlayer(currentPlayer, "You played: " + cardCode);
        telegramBot.sendMessageToPlayer(opponent.getPlayer(), currentPlayer.getUsername() + " played: " + cardCode);
    }

    private void updateGameState(Game game, OnlinePlayer onlinePlayer, Card card) {
         onlinePlayer.getCards().removeIf(c -> c.getCode().equals(card.getCode()));
        Attack attack = attackRepository.findByGame(game);
        List<Card> offensiveCards = attack.getOffensiveCards();
        offensiveCards.add(card);
        attack.setOffensiveCards(offensiveCards);
        attackRepository.save(attack);
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
