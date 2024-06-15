package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private final AttackRepository attackRepository;
    private final TelegramBot telegramBot;

    public Attack createAttack(Game game) {
        OnlinePlayer attacker = countWhoAttackFirst(game);
        game.setActivePlayer(attacker);
        telegramBot.showAvailableCards(attacker.getPlayer().getChatId(), attacker.getCards());
        telegramBot.sendMessageToPlayer(attacker.getPlayer(), "now is your move!");
        telegramBot.sendMessageToPlayer(getDefender(game).getPlayer(), "now is " + attacker.getPlayer().getUsername() + " move");
        return Attack.builder()
                .attacker(attacker)
                .defender(getDefender(game))
                .activePlayer(attacker)
                .game(game)
                .build();
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
        return attackRepository.findActivePlayerIdByAttackId(attackId);
    }

    @Override
    public void finishAttack(OnlinePlayer onlinePlayer) {

    }

    @Override
    public void makeMove() {

    }

    public OnlinePlayer getDefender(Game game) {
        List<OnlinePlayer> onlinePlayers = game.getPlayers();
        OnlinePlayer attackFirst = countWhoAttackFirst(game);
        int index = onlinePlayers.indexOf(attackFirst);
        int defenderIndex = (index + 1) % onlinePlayers.size();
        return onlinePlayers.get(defenderIndex);
    }
}
