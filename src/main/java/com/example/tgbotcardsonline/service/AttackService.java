package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;

import java.util.List;

public interface AttackService {

    Attack createAttack(Game game);

    OnlinePlayer countWhoAttackFirst(List<OnlinePlayer> onlinePlayerList, Game game);

    Long getActivePlayerId(Long attackId);
}
