package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;

public interface GameService {

    Game createGame1v1ThrowIn(Player firstPlayer, Player secondPlayer);

    void surrend(OnlinePlayer player);


    void makeMove(Player player, Card playerMove);

    void attackMove(Game game, Card move);

    void defenceMove(Game game, Card move);

    void finishAttack(Player player, Game game);

    void takeCards(Player player);
}
