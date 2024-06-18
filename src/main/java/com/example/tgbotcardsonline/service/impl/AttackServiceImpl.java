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

import java.util.*;
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
        //TODO: !!!
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
//TODO: FIX
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
        gameRepository.save(game);
    }

    private void handleGameOver(Game game) {
        for (OnlinePlayer player : game.getPlayers()) {
            Player p = player.getPlayer();
            p.setInGame(false);
            telegramBot.sendMessageToPlayer(p, "Game over!");
        }
    }
//TODO: check deck
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

    }

    public static OnlinePlayer getNextPlayer(Game game){
        List<OnlinePlayer> players = game.getPlayers();
        Optional<OnlinePlayer> onlinePlayer = players.stream().filter(oP -> oP.equals(oP.getGame().getActivePlayer())).findFirst();
        int index = players.indexOf(onlinePlayer.get());
        if(index == players.size()-1){
           return players.get(0);
        }else{
            return players.get(index+1);
        }
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
