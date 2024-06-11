package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.Player;

public interface GameService {
    Game createGame(Player firstPlayer, Player secondPlayer);
}
