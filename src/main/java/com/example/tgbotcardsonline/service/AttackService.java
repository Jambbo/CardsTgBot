package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;

import java.util.List;

public interface AttackService {

    Attack createAttack(Game game);



    OnlinePlayer countWhoAttackFirst(Game game);

    Long getActivePlayerId(Long attackId);

    void finishAttack(OnlinePlayer onlinePlayer);

    void makeMove();
}
