package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.*;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import com.example.tgbotcardsonline.web.mapper.CardMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
    private final DeckResponseRepository deckResponseRepository;

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
        Attack attack = game.getCurrentAttack();
        Player currentPlayer = onlinePlayer.getPlayer();
        Boolean  success;
        if (!isPlayerTurn(onlinePlayer)) {
            telegramBot.sendMessageToPlayer(currentPlayer, "It's not your turn!");
            return;
        }
        //TODO: !!!
        Card card = getInputedCard(onlinePlayer, callBackData);
        if (isNull(card)) {
            throw new IllegalArgumentException("Invalid card code: " + callBackData);
        }
        if (onlinePlayer.equals(attack.getDefender())) { // if defend
            success = handleDefend(attack, onlinePlayer, card);
            if(!success) return; // if defence was not  successful -> finish method
        } else { // if attack
            setAttackCards(attack, onlinePlayer, card);
        }


        notifyPlayers(game, onlinePlayer, callBackData);
        if (isGameOver(game)) {
            handleGameOver(game);

        } else {
            // Switch turns
            switchTurns(game);
        }

        gameRepository.save(game);
    }

    private boolean handleDefend(Attack attack, OnlinePlayer onlinePlayer, Card defendingCard) {
        List<Card> offensiveCards = attack.getOffensiveCards();
        Suit trumpSuit = attack.getGame().getTrump();  // Assuming this method exists to get the trump suit

        for (Card offensiveCard : offensiveCards) {
            if (canDefend(offensiveCard, defendingCard, trumpSuit)) {
                // TODO IMPLEMENT
                System.out.println("Defense successful!");
                return true;
            } else {
                telegramBot.sendMessageToPlayer(onlinePlayer.getPlayer(), "This card cannot defence!");
                return false;
            }
        }
        return false;
    }

    private boolean canDefend(Card offensiveCard, Card defendingCard, Suit trumpSuit) {
        if (defendingCard.getSuit() == offensiveCard.getSuit()) {
            // If both cards are of the same suit, compare by value
            return defendingCard.getValue().ordinal() > offensiveCard.getValue().ordinal();
        } else if (defendingCard.getSuit() == trumpSuit) {
            // If the defending card is a trump card, it beats any non-trump card
            return offensiveCard.getSuit() != trumpSuit;
        } else {
            return false;
        }
    }

    @SneakyThrows
    private Card getInputedCard(OnlinePlayer onlinePlayer, String callBackData) {
        List<Card> cards = onlinePlayer.getCards();
        for (Card card : cards) {
            if (card.getCode().equals(callBackData)) return card;
        }
        return null;
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

    private void setAttackCards(Attack attack, OnlinePlayer onlinePlayer, Card card) {
        onlinePlayer.getCards().removeIf(c -> c.getCode().equals(card.getCode()));
        attack.getOffensiveCards().add(card);
        onlinePlayerRepository.save(onlinePlayer);
        attackRepository.save(attack);
    }

    public static OnlinePlayer getNextPlayer(Game game) {
        List<OnlinePlayer> players = game.getPlayers();
        Optional<OnlinePlayer> onlinePlayer = players.stream().filter(oP -> oP.equals(oP.getGame().getActivePlayer())).findFirst();
        int index = players.indexOf(onlinePlayer.get());
        if (index == players.size() - 1) {
            return players.get(0);
        } else {
            return players.get(index + 1);
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
