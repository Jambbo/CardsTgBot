package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.service.AttackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private final AttackRepository attackRepository;

    public Attack createAttack(Game game){
        OnlinePlayer attacker = countWhoAttackFirst(game.getPlayers(), game);
        Attack attack = Attack.builder()
                .attacker(attacker)
                .defender(getDefender(game.getPlayers(),game))
                .activePlayer(attacker)
                .build();
        game.setActivePlayer(attacker);
        attack.setGame(game);
        return attack;
    }
    @Override
    public OnlinePlayer countWhoAttackFirst(List<OnlinePlayer> onlinePlayers, Game game) {
        Suit trump = game.getTrump();
        AtomicReference<OnlinePlayer> firstAttacker = new AtomicReference<>(null);
        AtomicReference<Card> lowestTrumpCard = new AtomicReference<>(null);
        onlinePlayers.forEach(oP -> {
            oP.getCards().forEach(c -> {
                c.getSuit();
                boolean isTrump = c.isTrump(trump);
                if(lowestTrumpCard.get() == null || c.getValue().compareTo(lowestTrumpCard.get().getValue())<6){
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

    public OnlinePlayer getDefender(List<OnlinePlayer> onlinePlayers, Game game){
        OnlinePlayer attackFirst = countWhoAttackFirst(onlinePlayers, game);
        int index = onlinePlayers.indexOf(attackFirst);
        int defenderIndex = (index + 1) % onlinePlayers.size();
        return onlinePlayers.get(defenderIndex);
    }
}
