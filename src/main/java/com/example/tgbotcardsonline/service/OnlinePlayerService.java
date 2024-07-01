package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;

public interface OnlinePlayerService {

    OnlinePlayer createOnlinePlayer(Player player, String deckId);


}
